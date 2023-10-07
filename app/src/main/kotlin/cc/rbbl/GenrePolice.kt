package cc.rbbl

import cc.rbbl.exceptions.ParsingException
import cc.rbbl.link_handlers.SpotifyMessageHandler
import cc.rbbl.persistence.MessageDao
import cc.rbbl.persistence.MessageEntity
import net.dv8tion.jda.api.entities.EmbedType
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.entities.MessageEmbed
import net.dv8tion.jda.api.entities.MessageEmbed.Field
import net.dv8tion.jda.api.entities.MessageEmbed.Footer
import net.dv8tion.jda.api.events.*
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.ErrorResponse
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.OffsetDateTime
import kotlin.system.exitProcess

class GenrePolice(config: ProgramConfig) : ListenerAdapter(), Runnable {
    private val oldStatusCommand = "stats"
    private val statusCommand = "status"
    private val okErrorResponses = listOf(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL)
    private var isConnected = true
    private val log = LoggerFactory.getLogger(GenrePolice::class.java)
    private val messageHandlers: Array<MessageHandler>

    init {
        messageHandlers = arrayOf(SpotifyMessageHandler(config))
    }

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)
        event.jda.retrieveCommands().queue {
            it.forEach { command ->
                if (command.name == oldStatusCommand) {
                    event.jda.deleteCommandById(command.id).queue()
                }
            }
        }
        event.jda.upsertCommand(statusCommand, "Get global Stats of the Genre-Police Bot.").queue()
        HealthAttributes.discord = true
        isConnected = true
    }

    override fun onDisconnect(event: DisconnectEvent) {
        super.onDisconnect(event)
        HealthAttributes.discord = false
        isConnected = false
        val thread = Thread(this)
        log.error("Disconnected. Starting reconnect timeout of $RECONNECTION_TIMEOUT mills")
        thread.start()
    }

    override fun onReconnected(event: ReconnectedEvent) {
        super.onReconnected(event)
        HealthAttributes.discord = true
        isConnected = true
        log.info("Reconnected!")
    }

    override fun onResumed(event: ResumedEvent) {
        super.onResumed(event)
        HealthAttributes.discord = true
        isConnected = true
        log.info("Reconnected!")
    }

    override fun onShutdown(event: ShutdownEvent) {
        super.onShutdown(event)
        log.error("Discord Shutdown Event occurred. Shutting down Process.")
        exitProcess(1)
    }

    override fun onMessageReceived(event: MessageReceivedEvent) {
        val msg = event.message
        if (msg.contentRaw.contains("genre", true)) {
            return
        }
        val responses = ArrayList<ResponseData>()
        for (handler in messageHandlers) {
            responses.addAll(handler.getGenreResponses(msg.contentRaw))
        }
        metadataToEmbeds(responses).forEach {
            msg.replyEmbeds(it).queue { sendMessage: Message ->
                transaction {
                    MessageDao.new(sendMessage.idLong) {
                        sourceMessageId = msg.idLong
                        sourceMessageAuthor = msg.author.idLong
                    }
                }
                sendMessage.addReaction(DELETE_REACTION).queue()
                log.info("Send Message ${sendMessage.id}")
            }
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.reaction.reactionEmote.asCodepoints == DELETE_REACTION) {
            val entity = transaction {
                MessageDao.findById(event.messageIdLong)
            }
            if (entity != null && entity.sourceMessageAuthor == event.userIdLong) {
                log.info("Deleting Message ${entity.id}")
                event.retrieveMessage().queue { message: Message ->
                    message.delete().queue {
                        transaction {
                            entity.isDeleted = true
                        }
                    }
                }
            }
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) = transaction {
        val resultList = MessageDao.find(MessageEntity.sourceMessageId eq event.messageIdLong)
        for (entity in resultList) {
            log.info("Deleting Message " + entity.id)
            event.channel.deleteMessageById(entity.id.value).queue({
                transaction {
                    entity.isDeleted = true
                }
            }) { throwable: Throwable? ->
                if (throwable is ErrorResponseException) {
                    if (okErrorResponses.contains(throwable.errorResponse)) {
                        transaction {
                            entity.isDeleted = true
                        }
                    }
                } else {
                    throw RuntimeException(throwable)
                }
            }
        }
    }

    override fun onSlashCommand(event: SlashCommandEvent) {
        if (event.name == statusCommand) {
            event.deferReply().queue() // Tell discord we received the command, send a thinking... message to the user
            val result = StatsRepository.getStats()
            event.hook.sendMessage(
                """**Servers**: ${result.serverCount}
                   **Messages**: ${result.messageCount}
                   **Version**: ${result.appVersion}""".trimIndent()).queue()
        }
    }

    override fun run() {
        try {
            Thread.sleep(RECONNECTION_TIMEOUT)
            if (!isConnected) {
                log.error("Shutting down after waiting $RECONNECTION_TIMEOUT mills to reconnect.")
                exitProcess(1)
            }
        } catch (e: InterruptedException) {
            if (!isConnected) {
                log.error("Reconnection Thread interrupted. Shutting down immediately.", e)
                exitProcess(1)
            }
        }
    }

    private fun metadataToEmbeds(responseSet: List<ResponseData>): List<MessageEmbed> {
        return responseSet.distinct().map { responseDataToMessageEmbed(it) }
    }

    private fun responseDataToMessageEmbed(data: ResponseData): MessageEmbed {
        if (data.error != null) {
            val errorText = when (data.error) {
                is IllegalArgumentException -> {
                    "unknown ID"
                }

                is ParsingException -> {
                    "broken Link"
                }

                else -> {
                    "unknown Error"
                }
            } + " for ${data.url}"
            return MessageEmbed(
                null, "Error", errorText, null, null, DEFAULT_EMBED_COLOR, null, null, null, null, null, null, null
            )
        }
        return if (data.imageHeightAndWidth != null) {
            MessageEmbed(
                data.url,
                data.title,
                data.getDescription(),
                EmbedType.RICH,
                OffsetDateTime.now(),
                DEFAULT_EMBED_COLOR,
                MessageEmbed.Thumbnail(data.titleImageUrl, null, data.imageHeightAndWidth, data.imageHeightAndWidth),
                null,
                MessageEmbed.AuthorInfo(data.artists?.firstOrNull()?.name, data.artists?.firstOrNull()?.url, data.artistImageUrl, null),
                null,
                DEFAULT_FOOTER,
                null,
                metadataToEmbedFields(data.metadata)
            )
        } else {
            MessageEmbed(
                data.url,
                data.title,
                data.getDescription(),
                EmbedType.RICH,
                OffsetDateTime.now(),
                DEFAULT_EMBED_COLOR,
                null,
                null,
                MessageEmbed.AuthorInfo(data.artists?.firstOrNull()?.name, data.artists?.firstOrNull()?.url, data.artistImageUrl, null),
                null,
                DEFAULT_FOOTER,
                null,
                metadataToEmbedFields(data.metadata)
            )
        }
    }

    private fun metadataToEmbedFields(metadata: Map<String, List<String>>?): List<Field> {
        if (metadata == null) {
            return emptyList()
        }
        return metadata.map { Field(it.key, it.value.joinToString(), false, true) }
    }

    companion object {
        private const val DELETE_REACTION = "U+1f5d1"
        private const val RECONNECTION_TIMEOUT = 70000L
        private const val DEFAULT_EMBED_COLOR = 15277667
        private val DEFAULT_FOOTER = Footer("by rbbl.cc", null, null)
    }
}
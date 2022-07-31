package cc.rbbl

import cc.rbbl.exceptions.NoGenreFoundException
import cc.rbbl.exceptions.ParsingException
import cc.rbbl.link_handlers.SpotifyMessageHandler
import cc.rbbl.persistence.MessageDao
import cc.rbbl.persistence.MessageEntity
import cc.rbbl.program_parameters_jvm.ParameterHolder
import net.dv8tion.jda.api.entities.Message
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
import java.util.stream.Collectors
import kotlin.system.exitProcess

class GenrePolice(parameters: ParameterHolder) : ListenerAdapter(),
    Runnable {
    private val statsCommand = "stats"
    private val okErrorResponses = listOf(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL)
    private var isConnected = true
    private val log = LoggerFactory.getLogger(GenrePolice::class.java)
    private val messageHandlers: Array<MessageHandler>

    init {
        messageHandlers = arrayOf(SpotifyMessageHandler(parameters))
    }

    override fun onReady(event: ReadyEvent) {
        super.onReady(event)
        event.jda.upsertCommand(statsCommand, "Get global Stats of the Genre-Police Bot.").queue()
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
        val responses = ArrayList<GenreResponse>()
        for (handler in messageHandlers) {
            responses.addAll(handler.getGenreResponses(msg.contentRaw))
        }
        if (responses.size > 0) {
            msg.reply(responsesToMessage(responses)).queue { sendMessage: Message ->
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
        if (event.name == statsCommand) {
            event.deferReply().queue() // Tell discord we received the command, send a thinking... message to the user
            val messageCount: Long = transaction {
                MessageDao.count()
            }
            event.hook.sendMessage("**Servers**: ${event.jda.guilds.size}\n**Messages**: $messageCount").queue()
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

    private fun responsesToMessage(responseSet: List<GenreResponse>): String {
        var message = "Following Genres were found:\n"
        for ((title, genres, error) in responseSet.stream().distinct().collect(Collectors.toList())) {
            message += "**$title**:"
            if (error != null) {
                message += when (error) {
                    is NoGenreFoundException -> {
                        " Spotify has no genre for that Item"
                    }

                    is IllegalArgumentException -> {
                        " unknown ID"
                    }

                    is ParsingException -> {
                        " broken Link"
                    }

                    else -> {
                        " unknown Error"
                    }
                }
            } else {
                for (genre in genres) {
                    message += " \"$genre\""
                }
            }
            message += "\n"
        }
        return message
    }

    companion object {
        private const val DELETE_REACTION = "U+1f5d1"
        private const val RECONNECTION_TIMEOUT = 70000L
    }
}
package cc.rbbl

import cc.rbbl.exceptions.NoGenreFoundException
import cc.rbbl.exceptions.ParsingException
import cc.rbbl.link_handlers.SpotifyMessageHandler
import cc.rbbl.persistence.MessageEntity
import cc.rbbl.program_parameters_jvm.ParameterHolder
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.DisconnectEvent
import net.dv8tion.jda.api.events.ReconnectedEvent
import net.dv8tion.jda.api.events.ResumedEvent
import net.dv8tion.jda.api.events.ShutdownEvent
import net.dv8tion.jda.api.events.message.MessageDeleteEvent
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.api.exceptions.ErrorResponseException
import net.dv8tion.jda.api.hooks.ListenerAdapter
import net.dv8tion.jda.api.requests.ErrorResponse
import org.slf4j.LoggerFactory
import java.util.stream.Collectors
import javax.persistence.EntityManagerFactory
import kotlin.system.exitProcess

class GenrePolice(parameters: ParameterHolder, entityManagerFactory: EntityManagerFactory) : ListenerAdapter(),
    Runnable {
    private val okErrorResponses = listOf(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL)
    private var isConnected = true
    private val log = LoggerFactory.getLogger(GenrePolice::class.java)
    private val messageHandlers: Array<MessageHandler>
    private val entityManagerFactory: EntityManagerFactory

    init {
        messageHandlers = arrayOf(SpotifyMessageHandler(parameters))
        this.entityManagerFactory = entityManagerFactory
    }

    override fun onDisconnect(event: DisconnectEvent) {
        super.onDisconnect(event)
        isConnected = false
        val thread = Thread(this)
        log.error("Disconnected. Starting reconnect timeout of $RECONNECTION_TIMEOUT mills")
        thread.start()
    }

    override fun onReconnected(event: ReconnectedEvent) {
        super.onReconnected(event)
        isConnected = true
        log.info("Reconnected!")
    }

    override fun onResumed(event: ResumedEvent) {
        super.onResumed(event)
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
                val entityManager = entityManagerFactory.createEntityManager()
                entityManager.transaction.begin()
                entityManager.persist(MessageEntity(sendMessage.idLong, msg.idLong, msg.author.idLong, false))
                entityManager.transaction.commit()
                entityManager.close()
                sendMessage.addReaction(DELETE_REACTION).queue()
                log.info("Send Message ${sendMessage.id}")
            }
        }
    }

    override fun onMessageReactionAdd(event: MessageReactionAddEvent) {
        if (event.reaction.reactionEmote.asCodepoints == DELETE_REACTION) {
            val entityManager = entityManagerFactory.createEntityManager()
            val entity = entityManager.find(MessageEntity::class.java, event.messageIdLong)
            if (entity != null && entity.sourceMessageAuthor == event.userIdLong) {
                log.info("Deleting Message ${entity.id}")
                event.retrieveMessage().queue { message: Message ->
                    message.delete().queue {
                        entityManager.transaction.begin()
                        val editEntity = entityManager.find(MessageEntity::class.java, message.idLong)
                        editEntity.isDeleted = true
                        entityManager.merge(editEntity)
                        entityManager.transaction.commit()
                        entityManager.close()
                    }
                }
            }
        }
    }

    override fun onMessageDelete(event: MessageDeleteEvent) {
        val entityManager = entityManagerFactory.createEntityManager()
        val resultList = entityManager
            .createQuery("FROM sent_messages WHERE sourceMessageId = ${event.messageId}", MessageEntity::class.java)
            .resultList
        for (entity in resultList) {
            log.info("Deleting Message " + entity.id)
            event.channel.deleteMessageById(entity.id).queue({
                entityManager.transaction.begin()
                entity.isDeleted = true
                entityManager.merge(entity)
                entityManager.transaction.commit()
                entityManager.close()
            }) { throwable: Throwable? ->
                if (throwable is ErrorResponseException) {
                    if (okErrorResponses.contains(throwable.errorResponse)) {
                        entityManager.transaction.begin()
                        entity.isDeleted = true
                        entityManager.merge(entity)
                        entityManager.transaction.commit()
                        entityManager.close()
                    }
                } else {
                    entityManager.close()
                    throw RuntimeException(throwable)
                }
            }
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
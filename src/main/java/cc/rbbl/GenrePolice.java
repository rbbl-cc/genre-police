package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;
import cc.rbbl.exceptions.ParsingException;
import cc.rbbl.link_handlers.SpotifyMessageHandler;
import cc.rbbl.persistence.MessageEntity;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.message.react.MessageReactionAddEvent;
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GenrePolice extends ListenerAdapter implements Runnable {

    private static final String DELETE_REACTION = "U+274c";
    private static final long RECONNECTION_TIMEOUT = 70000L;

    private final List<ErrorResponse> okErrorResponses = List.of(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL);

    private boolean isConnected = true;
    private static final Logger log = LoggerFactory.getLogger(GenrePolice.class);
    private final MessageHandler[] messageHandlers;
    private final EntityManagerFactory entityManagerFactory;

    public GenrePolice(ParameterHolder parameters, EntityManagerFactory entityManagerFactory) {
        messageHandlers = new MessageHandler[]{new SpotifyMessageHandler(parameters)};
        this.entityManagerFactory = entityManagerFactory;
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        super.onDisconnect(event);
        isConnected = false;
        Thread thread = new Thread(this);
        log.error("Disconnected. Starting reconnect timeout of " + RECONNECTION_TIMEOUT + " mills");
        thread.start();
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        super.onReconnected(event);
        isConnected = true;
        log.info("Reconnected!");
    }

    @Override
    public void onResumed(@NotNull ResumedEvent event) {
        super.onResumed(event);
        isConnected = true;
        log.info("Reconnected!");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        super.onShutdown(event);
        log.error("Discord Shutdown Event occurred. Shutting down Process.");
        System.exit(1);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        ArrayList<GenreResponse> responses = new ArrayList<>();
        for (MessageHandler handler : messageHandlers) {
            responses.addAll(handler.getGenreResponses(msg.getContentRaw()));
        }
        if (responses.size() > 0) {
            msg.reply(responsesToMessage(responses)).queue(sendMessage -> {
                EntityManager entityManager = entityManagerFactory.createEntityManager();
                entityManager.getTransaction().begin();
                entityManager.persist(new MessageEntity(sendMessage.getIdLong(), msg.getIdLong(), msg.getAuthor().getIdLong()));
                entityManager.getTransaction().commit();
                entityManager.close();
                sendMessage.addReaction(DELETE_REACTION).queue();
                log.info("Send Message " + sendMessage.getId());
            });
        }
    }

    @Override
    public void onMessageReactionAdd(@NotNull MessageReactionAddEvent event) {
        if (event.getReaction().getReactionEmote().getAsCodepoints().equals(DELETE_REACTION)) {
            EntityManager entityManager = entityManagerFactory.createEntityManager();
            MessageEntity entity = entityManager.find(MessageEntity.class, event.getMessageIdLong());
            entityManager.close();
            if (entity != null && entity.getSourceMessageAuthor() == event.getUserIdLong()) {
                event.retrieveMessage().queue(message -> message.delete().queue(unused -> {
                            EntityManager deleteEntityManager = entityManagerFactory.createEntityManager();
                            deleteEntityManager.getTransaction().begin();
                            MessageEntity editEntity = deleteEntityManager.find(MessageEntity.class, message.getIdLong());
                            editEntity.setDeleted(true);
                            deleteEntityManager.persist(editEntity);
                            deleteEntityManager.getTransaction().commit();
                        }
                ));
            }
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        EntityManager entityManager = entityManagerFactory.createEntityManager();
        List<MessageEntity> resultList = entityManager
                .createQuery("FROM sent_messages WHERE sourceMessageId = " + event.getMessageId(), MessageEntity.class)
                .getResultList();
        entityManager.close();
        for (MessageEntity entity : resultList) {
            event.getChannel().deleteMessageById(entity.getId()).queue(unused -> {
                EntityManager deleteEntityManager = entityManagerFactory.createEntityManager();
                deleteEntityManager.getTransaction().begin();
                entity.setDeleted(true);
                deleteEntityManager.persist(entity);
                deleteEntityManager.getTransaction().commit();
                deleteEntityManager.close();
            }, throwable -> {
                if (throwable instanceof ErrorResponseException) {
                    ErrorResponseException exception = (ErrorResponseException) throwable;
                    if (okErrorResponses.contains(exception.getErrorResponse())) {
                        EntityManager deleteEntityManager = entityManagerFactory.createEntityManager();
                        deleteEntityManager.getTransaction().begin();
                        entity.setDeleted(true);
                        deleteEntityManager.persist(entity);
                        deleteEntityManager.getTransaction().commit();
                        deleteEntityManager.close();
                    }
                } else {
                    throw new RuntimeException(throwable);
                }
            });

        }
    }

    @Override
    public void run() {
        try {
            Thread.sleep(RECONNECTION_TIMEOUT);
            if (!isConnected) {
                log.error("Shutting down after waiting " + RECONNECTION_TIMEOUT + " mills to reconnect.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            if (!isConnected) {
                log.error("Reconnection Thread interrupted. Shutting down immediately.", e);
                System.exit(1);
            }
        }
    }

    private String responsesToMessage(List<GenreResponse> responseSet) {
        StringBuilder message = new StringBuilder("Following Genres got found:\n");
        responseSet = responseSet.stream().distinct().collect(Collectors.toList());
        for (GenreResponse response : responseSet) {
            message.append("**").append(response.getTitle()).append("**").append(":");
            if (response.getError() != null) {
                if (response.getError() instanceof NoGenreFoundException) {
                    message.append(" Spotify has no genre for that Item");
                } else if (response.getError() instanceof IllegalArgumentException) {
                    message.append(" unknown ID");
                } else if (response.getError() instanceof ParsingException) {
                    message.append(" broken Link");
                } else {
                    message.append(" unknown Error");
                }
            } else {
                for (String genre : response.getGenres()) {
                    message.append(" \"").append(genre).append("\"");
                }
            }
            message.append("\n");
        }

        return message.toString();
    }
}

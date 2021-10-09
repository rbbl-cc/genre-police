package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;
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
import net.dv8tion.jda.api.exceptions.ErrorResponseException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.ErrorResponse;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class GenrePolice extends ListenerAdapter implements Runnable {

    private static final long RECONNECTION_TIMEOUT = 70000L;

    private final List<ErrorResponse> okErrorResponses = List.of(ErrorResponse.UNKNOWN_MESSAGE, ErrorResponse.UNKNOWN_CHANNEL);

    private boolean isConnected = true;
    private static final Logger log = LoggerFactory.getLogger(GenrePolice.class);
    private final MessageHandler[] messageHandlers;
    private final SessionFactory sessionFactory;

    public GenrePolice(ParameterHolder parameters, SessionFactory sessionFactory) {
        messageHandlers = new MessageHandler[]{new SpotifyMessageHandler(parameters)};
        this.sessionFactory = sessionFactory;
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
            msg.reply(responsesToMessage(responses)).queue(message -> {
                Session session = sessionFactory.openSession();
                Transaction transaction = session.beginTransaction();
                session.persist(new MessageEntity(message.getIdLong(), msg.getIdLong()));
                transaction.commit();
                session.close();
            });
        }
    }

    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent event) {
        Session session = sessionFactory.openSession();
        List<MessageEntity> resultList = session
                .createQuery("FROM sent_messages WHERE sourceMessageId = " + event.getMessageId(), MessageEntity.class)
                .getResultList();
        session.close();
        for (MessageEntity entity : resultList) {
            event.getChannel().deleteMessageById(entity.getId()).queue(unused -> {
                Session deleteSession = sessionFactory.openSession();
                Transaction transaction = deleteSession.beginTransaction();
                entity.setDeleted(true);
                deleteSession.update(entity);
                transaction.commit();
                deleteSession.close();
            }, throwable -> {
                if (throwable instanceof ErrorResponseException) {
                    ErrorResponseException exception = (ErrorResponseException) throwable;
                    if (okErrorResponses.contains(exception.getErrorResponse())) {
                        Session deleteSession = sessionFactory.openSession();
                        Transaction transaction = deleteSession.beginTransaction();
                        entity.setDeleted(true);
                        deleteSession.update(entity);
                        transaction.commit();
                        deleteSession.close();
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
            if(response.getError() != null) {
                if (response.getError() instanceof NoGenreFoundException) {
                    message.append(" Spotify has no genre for that Item");
                } else if (response.getError() instanceof IllegalArgumentException) {
                    message.append(" unknown ID");
                } else {
                    message.append(" broken Link");
                }
            }else {
                for (String genre : response.getGenres()) {
                    message.append(" \"").append(genre).append("\"");
                }
            }
            message.append("\n");
        }

        return message.toString();
    }
}

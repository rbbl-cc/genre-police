package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;
import cc.rbbl.program_parameters_jvm.ParameterDefinition;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.DisconnectEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.ResumedEvent;
import net.dv8tion.jda.api.events.ShutdownEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.hc.core5.http.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Set;

public class GenrePolice extends ListenerAdapter implements Runnable {

    private static final long RECONNECTION_TIMEOUT = 70000L;

    private boolean isConnected = true;
    private static final Logger logger = LoggerFactory.getLogger(GenrePolice.class);
    private final SpotifyLinkHandler spotifyLinkHandler;

    public GenrePolice(ParameterHolder parameters) throws ParseException, SpotifyWebApiException, IOException {
        spotifyLinkHandler = new SpotifyLinkHandler(parameters);
    }

    @Override
    public void onDisconnect(@NotNull DisconnectEvent event) {
        super.onDisconnect(event);
        isConnected = false;
        Thread thread = new Thread(this);
        logger.error("Disconnected. Starting reconnect timeout of " + RECONNECTION_TIMEOUT + " mills");
        thread.start();
    }

    @Override
    public void onReconnected(@NotNull ReconnectedEvent event) {
        super.onReconnected(event);
        isConnected = true;
        logger.info("Reconnected!");
    }

    @Override
    public void onResumed(@NotNull ResumedEvent event) {
        super.onResumed(event);
        isConnected = true;
        logger.info("Reconnected!");
    }

    @Override
    public void onShutdown(@NotNull ShutdownEvent event) {
        super.onShutdown(event);
        logger.error("Discord Shutdown Event occurred. Shutting down Process.", event);
        System.exit(1);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        String response = null;
        try {
            response = genresToMessage(spotifyLinkHandler.getGenres(msg.getContentRaw()));
        } catch (NoGenreFoundException e) {
            response = "Spotify has no genre for that Item";
        } finally {
            if (response != null) {
                msg.reply(response).queue();
            }
        }
    }

    private String genresToMessage(String[] genres) {
        if (genres == null) {
            return null;
        }
        String message = "Genres: ";
        for (String genre : genres) {
            message += "\"" + genre + "\" ";
        }
        return message;
    }

    @Override
    public void run() {
        try {
            Thread.sleep(RECONNECTION_TIMEOUT);
            if (!isConnected) {
                logger.error("Shutting down after waiting " + RECONNECTION_TIMEOUT + " mills to reconnect.");
                System.exit(1);
            }
        } catch (InterruptedException e) {
            if (!isConnected) {
                logger.error("Reconnection Thread interrupted. Shutting down immediately.", e);
                System.exit(1);
            }
        }
    }
}

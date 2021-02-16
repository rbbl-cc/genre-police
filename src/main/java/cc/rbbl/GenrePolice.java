package cc.rbbl;

import cc.rbbl.exceptions.NoGenreFoundException;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.hc.core5.http.ParseException;

import javax.security.auth.login.LoginException;
import java.io.IOException;

public class GenrePolice extends ListenerAdapter {

    private final SpotifyLinkHandler spotifyLinkHandler;

    public static void main(String[] args) throws LoginException, IllegalArgumentException, ParseException, SpotifyWebApiException, IOException {
        ProgramParameters params = new ProgramParameters(args);

        // We only need 2 intents in this bot. We only respond to messages in guilds and private channels.
        // All other events will be disabled.
        JDABuilder.createLight(params.getDiscordToken(), GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new GenrePolice(params))
                .setActivity(Activity.watching("Spotify Links"))
                .build();
    }

    public GenrePolice(ProgramParameters parameters) throws ParseException, SpotifyWebApiException, IOException {
        spotifyLinkHandler = new SpotifyLinkHandler(parameters);
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        Message msg = event.getMessage();
        if (SpotifyLinkHandler.isApplicableMsg(msg.getContentRaw())) {
            String response = "";
            try {
                response = genresToMessage(spotifyLinkHandler.getGenres(msg.getContentRaw()));
            } catch (NoGenreFoundException e) {
                response = "Spotify has no genre for that Item";
            }finally {
                msg.reply(response).queue();
            }
        }
    }

    private String genresToMessage(String[] genres) {
        String message = "Genres: ";
        for (String genre : genres) {
            message += "\"" + genre + "\" ";
        }
        return message;
    }

    private boolean emptyOrNull(String[] input) {
        return input == null || input.length == 0;
    }
}

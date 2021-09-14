package cc.rbbl;

import cc.rbbl.program_parameters_jvm.ParameterDefinition;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.hc.core5.http.ParseException;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws LoginException, IllegalArgumentException, ParseException, SpotifyWebApiException, IOException {
        ParameterHolder params = new ParameterHolder(Set.of(
                new ParameterDefinition("DiscordToken", true, false),
                new ParameterDefinition("SpotifyClientID", true, false),
                new ParameterDefinition("SpotifyClientSecret", true, false)
        ));
        params.loadParametersFromEnvironmentVariables();
        params.loadParameters(args);
        params.checkParameterCompleteness();

        // We only need 2 intents in this bot. We only respond to messages in guilds and private channels.
        // All other events will be disabled.
        JDABuilder.createLight(params.get("DiscordToken"), GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new GenrePolice(params))
                .setActivity(Activity.watching("Spotify Links"))
                .build();
    }
}

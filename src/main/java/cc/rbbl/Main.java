package cc.rbbl;

import cc.rbbl.persistence.MessageEntity;
import cc.rbbl.program_parameters_jvm.ParameterDefinition;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.apache.hc.core5.http.ParseException;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.Configuration;

import javax.security.auth.login.LoginException;
import java.io.IOException;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws LoginException, IllegalArgumentException, ParseException, SpotifyWebApiException, IOException {
        ParameterHolder params = new ParameterHolder(Set.of(
                new ParameterDefinition("DISCORD_TOKEN", true, false),
                new ParameterDefinition("SPOTIFY_CLIENT_ID", true, false),
                new ParameterDefinition("SPOTIFY_CLIENT_SECRET", true, false),
                new ParameterDefinition("DB_USER", true, false),
                new ParameterDefinition("DB_PASSWORD", true, false),
                new ParameterDefinition("JDBC_URL", true, false)
        ));
        params.loadParametersFromEnvironmentVariables();
        params.loadParameters(args);
        params.checkParameterCompleteness();

        handleDbMigration(params);

        // We only need 2 intents in this bot. We only respond to messages in guilds and private channels.
        // All other events will be disabled.
        JDABuilder.createLight(params.get("DISCORD_TOKEN"), GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES)
                .addEventListeners(new GenrePolice(params, getHibernateConfig(params).buildSessionFactory()))
                .setActivity(Activity.watching("Spotify Links"))
                .build();
    }

    private static void handleDbMigration(ParameterHolder parameters) {
        Flyway flyway = Flyway.configure().dataSource(parameters.get("JDBC_URL"), parameters.get("DB_USER"), parameters.get("DB_PASSWORD")).load();
        flyway.migrate();
    }

    private static Configuration getHibernateConfig(ParameterHolder parameters) {
        Configuration config = new Configuration();
        config.setProperty("hibernate.connection.password", parameters.get("DB_PASSWORD"));
        config.setProperty("hibernate.connection.driver_class", "org.postgresql.Driver");
        config.setProperty("hibernate.connection.url", parameters.get("JDBC_URL"));
        config.setProperty("hibernate.connection.username", parameters.get("DB_USER"));
        config.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        config.addAnnotatedClass(MessageEntity.class);
        return config;
    }
}

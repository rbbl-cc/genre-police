package cc.rbbl;

import cc.rbbl.persistence.MessageEntity;
import cc.rbbl.program_parameters_jvm.ParameterDefinition;
import cc.rbbl.program_parameters_jvm.ParameterHolder;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.commons.dbcp2.BasicDataSource;
import org.flywaydb.core.Flyway;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.cfg.Configuration;

import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.security.auth.login.LoginException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Main {
    public static void main(String[] args) throws LoginException, IllegalArgumentException {
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
        JDABuilder.create(params.get("DISCORD_TOKEN"), GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES,
                        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS)
                .disableCache(CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE,
                        CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS)
                .addEventListeners(new GenrePolice(params, getHibernateConfig(params)))
                .setActivity(Activity.watching("Spotify Links"))
                .build();
    }

    private static void handleDbMigration(ParameterHolder parameters) {
        Flyway flyway = Flyway.configure().dataSource(
                parameters.get("JDBC_URL"),
                parameters.get("DB_USER"),
                parameters.get("DB_PASSWORD")
        ).load();
        flyway.migrate();
    }

    private static EntityManagerFactory getHibernateConfig(ParameterHolder parameters) {
        BasicDataSource dataSource = new BasicDataSource();

        dataSource.setPassword(parameters.get("DB_PASSWORD"));
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(parameters.get("JDBC_URL"));
        dataSource.setUsername(parameters.get("DB_USER"));

        Map<String, Object> properties = new HashMap<>();
        properties.put(AvailableSettings.DATASOURCE, dataSource);
        properties.put(AvailableSettings.DIALECT, "org.hibernate.dialect.PostgreSQLDialect");
        return Persistence.createEntityManagerFactory("p-unit", properties);


    }
}

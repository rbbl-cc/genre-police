package cc.rbbl

import cc.rbbl.program_parameters_jvm.ParameterDefinition
import cc.rbbl.program_parameters_jvm.ParameterHolder
import io.ktor.application.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ktor_health_check.Health
import net.dv8tion.jda.api.JDABuilder
import net.dv8tion.jda.api.entities.Activity
import net.dv8tion.jda.api.requests.GatewayIntent
import net.dv8tion.jda.api.utils.cache.CacheFlag
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.transaction

fun main(args: Array<String>) {
    val params = ParameterHolder(
        setOf(
            ParameterDefinition("DISCORD_TOKEN", caseSensitive = false),
            ParameterDefinition("SPOTIFY_CLIENT_ID", caseSensitive = false),
            ParameterDefinition("SPOTIFY_CLIENT_SECRET", caseSensitive = false),
            ParameterDefinition("DB_USER", caseSensitive = false),
            ParameterDefinition("DB_PASSWORD", caseSensitive = false),
            ParameterDefinition("JDBC_URL", caseSensitive = false)
        )
    )
    params.loadParametersFromEnvironmentVariables()
    params.loadParameters(args)
    params.checkParameterCompleteness()

    handleDbMigration(params)

    Database.connect(params["JDBC_URL"]!!, "org.postgresql.Driver", params["DB_USER"]!!, params["DB_PASSWORD"]!!)

    // We only need 2 intents in this bot. We only respond to messages in guilds and private channels.
    // All other events will be disabled.
    val jda = JDABuilder.create(
        params["DISCORD_TOKEN"], GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS
    ).disableCache(
        CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE,
        CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS
    ).addEventListeners(GenrePolice(params))
        .setActivity(Activity.watching("Spotify Links"))
        .build()

    StatsRepository.jda = jda

    runBlocking {
        launch {
            transaction {
                HealthAttributes.database = !connection.isClosed
            }
            delay(60000) //one minute
        }
        launch {
            embeddedServer(Netty, port = 8080) {
                routing {
                    get("/") {
                        call.respondText("Hello, world!")
                    }
                }
                install(Health) {
                    readyCheck("database") {
                        HealthAttributes.database
                    }
                    healthCheck("database") {
                        HealthAttributes.database
                    }
                    readyCheck("discord") {
                        HealthAttributes.discord
                    }
                    healthCheck("discord") {
                        HealthAttributes.discord
                    }
                }
            }.start(wait = true)
        }
    }
}

private fun handleDbMigration(parameters: ParameterHolder) {
    val flyway = Flyway.configure().dataSource(
        parameters["JDBC_URL"],
        parameters["DB_USER"],
        parameters["DB_PASSWORD"]
    ).load()
    flyway.migrate()
}
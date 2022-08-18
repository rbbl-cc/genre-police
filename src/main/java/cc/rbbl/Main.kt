package cc.rbbl

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
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
    ProgramConfig.parameters.loadParametersFromEnvironmentVariables()
    ProgramConfig.parameters.loadParameters(args)
    ProgramConfig.parameters.checkParameterCompleteness()

    val config = ProgramConfig()
    handleDbMigration(config)

    Database.connect(config.jdbcUrl, "org.postgresql.Driver", config.dbUser, config.dbPassword)

    // We only need 2 intents in this bot. We only respond to messages in guilds and private channels.
    // All other events will be disabled.
    val jda = JDABuilder.create(
        config.discordToken, GatewayIntent.GUILD_MESSAGES, GatewayIntent.DIRECT_MESSAGES,
        GatewayIntent.GUILD_MESSAGE_REACTIONS, GatewayIntent.DIRECT_MESSAGE_REACTIONS
    ).disableCache(
        CacheFlag.ACTIVITY, CacheFlag.VOICE_STATE, CacheFlag.EMOTE,
        CacheFlag.CLIENT_STATUS, CacheFlag.ONLINE_STATUS
    ).addEventListeners(GenrePolice(config))
        .setActivity(Activity.watching("Spotify Links"))
        .build()

    StatsRepository.jda = jda

    if (config.webserverEnabled) {
        runBlocking {
            launch {
                transaction {
                    HealthAttributes.database = !connection.isClosed
                }
                delay(10000) //10 seconds
            }
            launch {
                embeddedServer(Netty, port = config.port) {
                    install(ContentNegotiation) {
                        json()
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
                    routing {
                        get("/stats") {
                            call.respond(StatsRepository.getStats())
                        }
                    }
                }.start(wait = true)
            }
        }
    }
}

private fun handleDbMigration(config: ProgramConfig) {
    val flyway = Flyway.configure().dataSource(
        config.jdbcUrl,
        config.dbUser,
        config.dbPassword
    ).load()
    flyway.migrate()
}
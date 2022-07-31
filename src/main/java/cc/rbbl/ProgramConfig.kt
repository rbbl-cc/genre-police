package cc.rbbl

import cc.rbbl.program_parameters_jvm.ParameterDefinition
import cc.rbbl.program_parameters_jvm.ParameterHolder
import org.slf4j.LoggerFactory

class ProgramConfig {
    private val log = LoggerFactory.getLogger(ProgramConfig::class.java)
    private val defaultWebserverValue = true
    private val defaultPortValue = 8080
    private val defaultStatsCacheValue = 10000L //10 sec

    companion object {
        val parameters = ParameterHolder(
            setOf(
                ParameterDefinition("DISCORD_TOKEN", caseSensitive = false),
                ParameterDefinition("SPOTIFY_CLIENT_ID", caseSensitive = false),
                ParameterDefinition("SPOTIFY_CLIENT_SECRET", caseSensitive = false),
                ParameterDefinition("DB_USER", caseSensitive = false),
                ParameterDefinition("DB_PASSWORD", caseSensitive = false),
                ParameterDefinition("JDBC_URL", caseSensitive = false),
                ParameterDefinition("WEBSERVER_ENABLED", false),
                ParameterDefinition("PORT", false),
                ParameterDefinition("STATS_CACHE_TIME_MS", false)
            )
        )
    }

    val discordToken: String = parameters["DISCORD_TOKEN"]!!
    val spotifyClientId: String = parameters["SPOTIFY_CLIENT_ID"]!!
    val spotifyClientSecret: String = parameters["SPOTIFY_CLIENT_SECRET"]!!
    val dbUser: String = parameters["DB_USER"]!!
    val dbPassword: String = parameters["DB_PASSWORD"]!!
    val jdbcUrl: String = parameters["JDBC_URL"]!!
    val webserverEnabled: Boolean = parameters["WEBSERVER_ENABLED"]?.toBooleanStrictOrNull().apply {
        if (this == null && parameters["WEBSERVER_ENABLED"] != null) {
            unparseableParameterWarning("WEBSERVER_ENABLED", defaultWebserverValue.toString())
        }
    } ?: defaultWebserverValue
    val port: Int = parameters["PORT"]?.toIntOrNull().apply {
        if (this == null && parameters["PORT"] != null) {
            unparseableParameterWarning("PORT", defaultPortValue.toString())
        }
    } ?: defaultPortValue
    val statsCacheTimeMs: Long = parameters["STATS_CACHE_TIME_MS"]?.toLongOrNull().apply {
        if (this == null && parameters["STATS_CACHE_TIME_MS"] != null) {
            unparseableParameterWarning("STATS_CACHE_TIME_MS", defaultStatsCacheValue.toString())
        }
    } ?: defaultStatsCacheValue

    fun unparseableParameterWarning(key: String, defaultValue: String) {
        log.warn("'$key' could not be parsed to a Boolean. Using Default: $defaultValue")
    }
}
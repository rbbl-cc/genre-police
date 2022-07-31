package cc.rbbl

import cc.rbbl.program_parameters_jvm.ParameterDefinition
import cc.rbbl.program_parameters_jvm.ParameterHolder

class ProgramConfig {
    companion object {
        val parameters = ParameterHolder(
            setOf(
                ParameterDefinition("DISCORD_TOKEN", caseSensitive = false),
                ParameterDefinition("SPOTIFY_CLIENT_ID", caseSensitive = false),
                ParameterDefinition("SPOTIFY_CLIENT_SECRET", caseSensitive = false),
                ParameterDefinition("DB_USER", caseSensitive = false),
                ParameterDefinition("DB_PASSWORD", caseSensitive = false),
                ParameterDefinition("JDBC_URL", caseSensitive = false)
            )
        )
    }

    val discordToken: String = parameters["DISCORD_TOKEN"]!!
    val spotifyClientId: String = parameters["SPOTIFY_CLIENT_ID"]!!
    val spotifyClientSecret: String = parameters["SPOTIFY_CLIENT_SECRET"]!!
    val dbUser: String = parameters["DB_USER"]!!
    val dbPassword: String = parameters["DB_PASSWORD"]!!
    val jdbcUrl: String = parameters["JDBC_URL"]!!
}
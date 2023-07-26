package cc.rbbl

import cc.rbbl.persistence.MessageDao
import kotlinx.serialization.Serializable
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.sql.transactions.transaction

class StatsRepository {
    companion object {
        lateinit var jda: JDA

        private val config: ProgramConfig = ProgramConfig()
        private var lastResult: StatsResult? = null
        private var timestamp = System.currentTimeMillis()

        fun getStats(): StatsResult {
            lastResult.apply {
                if (this == null) {
                    transaction {
                        lastResult = StatsResult(jda.guilds.size, MessageDao.count())
                        timestamp = System.currentTimeMillis()
                    }
                } else {
                    if (System.currentTimeMillis() - timestamp >= config.statsCacheTimeMs) {
                        transaction {
                            lastResult = StatsResult(jda.guilds.size, MessageDao.count())
                            timestamp = System.currentTimeMillis()
                        }
                    }
                }
            }
            return lastResult!!
        }
    }
}

@Serializable
data class StatsResult(val serverCount: Int, val messageCount: Long)
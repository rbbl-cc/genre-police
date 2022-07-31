package cc.rbbl

import cc.rbbl.persistence.MessageDao
import net.dv8tion.jda.api.JDA
import org.jetbrains.exposed.sql.transactions.transaction

class StatsRepository {
    companion object {
        lateinit var jda: JDA

        fun getStats(): StatsResult = transaction {
            StatsResult(jda.guilds.size, MessageDao.count())
        }
    }
}

data class StatsResult(val serverCount: Int, val messageCount: Long)
package cc.rbbl.persistence

import org.jetbrains.exposed.dao.id.LongIdTable

object MessageEntity : LongIdTable("sent_messages") {
    val sourceMessageId = long("source_message_id")
    val sourceMessageAuthor = long("source_message_author")
    val isDeleted = bool("deleted").default(false)
}
package cc.rbbl.persistence

import org.jetbrains.exposed.dao.LongEntity
import org.jetbrains.exposed.dao.LongEntityClass
import org.jetbrains.exposed.dao.id.EntityID

class MessageDao(id: EntityID<Long>) : LongEntity(id) {
    companion object : LongEntityClass<MessageDao>(MessageEntity)

    var sourceMessageId by MessageEntity.sourceMessageId
    var sourceMessageAuthor by MessageEntity.sourceMessageAuthor
    var isDeleted by MessageEntity.isDeleted
}
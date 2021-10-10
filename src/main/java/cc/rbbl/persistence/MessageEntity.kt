package cc.rbbl.persistence

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Id

@Entity(name = "sent_messages")
data class MessageEntity(
    @Id
    val id: Long = 0,

    @Column(name = "source_message_id")
    val sourceMessageId: Long = 0,

    @Column(name = "source_message_author")
    val sourceMessageAuthor: Long = 0,

    @Column(name = "deleted")
    var isDeleted: Boolean = false
)
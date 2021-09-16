package cc.rbbl.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity(name = "sent_messages")
public class MessageEntity {

    @Id
    private long id;

    @Column(name = "source_message_id")
    private long sourceMessageId;

    @Column
    private boolean deleted;

    protected MessageEntity(){
    }

    public MessageEntity(long id) {
        this.id = id;
    }

    public MessageEntity(long id, long sourceMessageId) {
        this.id = id;
        this.sourceMessageId = sourceMessageId;
    }

    public long getId() {
        return id;
    }

    public long getSourceMessageId() {
        return sourceMessageId;
    }

    public void setSourceMessageId(long sourceMessageId) {
        this.sourceMessageId = sourceMessageId;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDeleted() {
        return deleted;
    }

    public void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }
}

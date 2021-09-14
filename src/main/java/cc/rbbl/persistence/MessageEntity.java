package cc.rbbl.persistence;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;

@Entity
public class MessageEntity {

    @Id
    private long id;

    @Column(name = "source_message_id")
    private long sourceMessageId;
}

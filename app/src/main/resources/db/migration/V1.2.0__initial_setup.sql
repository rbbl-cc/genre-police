CREATE TABLE sent_messages (
    id bigint PRIMARY KEY,
    source_message_id bigint,
    deleted bool
)
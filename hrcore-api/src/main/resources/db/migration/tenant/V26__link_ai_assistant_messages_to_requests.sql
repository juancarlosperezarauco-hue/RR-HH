ALTER TABLE ai_messages
    ADD COLUMN in_reply_to_message_id UUID NULL;

ALTER TABLE ai_messages
    ADD CONSTRAINT fk_ai_messages_reply_to
        FOREIGN KEY (conversation_id, in_reply_to_message_id)
        REFERENCES ai_messages(conversation_id, id) ON DELETE CASCADE,
    ADD CONSTRAINT ck_ai_messages_user_cannot_be_reply CHECK (
        role <> 'USER' OR in_reply_to_message_id IS NULL
    );

CREATE UNIQUE INDEX uq_ai_messages_one_reply_per_request
    ON ai_messages(conversation_id, in_reply_to_message_id)
    WHERE in_reply_to_message_id IS NOT NULL;

CREATE TABLE ai_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id UUID NOT NULL,
    title VARCHAR(80) NOT NULL DEFAULT 'Nuevo informe',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',
    version INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_message_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_ai_conversations_owner
        FOREIGN KEY (owner_user_id) REFERENCES tenant_users(id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_conversations_status CHECK (status IN ('ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_ai_conversations_version CHECK (version >= 0),
    CONSTRAINT uq_ai_conversations_id_owner UNIQUE (id, owner_user_id)
);

CREATE INDEX idx_ai_conversations_owner_updated
    ON ai_conversations(owner_user_id, updated_at DESC, id DESC);

CREATE TABLE ai_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    client_message_id UUID NULL,
    role VARCHAR(16) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    message_type VARCHAR(20) NOT NULL DEFAULT 'TEXT',
    state VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ NULL,
    CONSTRAINT fk_ai_messages_conversation
        FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_messages_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT ck_ai_messages_type CHECK (
        message_type IN ('TEXT', 'REPORT', 'CLARIFICATION', 'UNSUPPORTED', 'ERROR')
    ),
    CONSTRAINT ck_ai_messages_state CHECK (state IN ('PENDING', 'COMPLETED', 'FAILED')),
    CONSTRAINT uq_ai_messages_conversation_id UNIQUE (conversation_id, id)
);

CREATE UNIQUE INDEX uq_ai_messages_client_id
    ON ai_messages(conversation_id, client_message_id)
    WHERE client_message_id IS NOT NULL;

CREATE INDEX idx_ai_messages_conversation_created
    ON ai_messages(conversation_id, created_at, id);

CREATE TABLE ai_report_runs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL,
    request_message_id UUID NOT NULL,
    query_plan JSONB NOT NULL,
    plan_version VARCHAR(20) NOT NULL,
    catalog_version VARCHAR(40) NOT NULL,
    state VARCHAR(16) NOT NULL,
    requested_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMPTZ NULL,
    result_metadata JSONB NULL,
    CONSTRAINT fk_ai_report_runs_conversation
        FOREIGN KEY (conversation_id) REFERENCES ai_conversations(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_report_runs_request_message
        FOREIGN KEY (conversation_id, request_message_id)
        REFERENCES ai_messages(conversation_id, id) ON DELETE CASCADE,
    CONSTRAINT ck_ai_report_runs_state CHECK (state IN ('SUCCEEDED', 'FAILED', 'INVALIDATED')),
    CONSTRAINT uq_ai_report_runs_conversation_id UNIQUE (conversation_id, id)
);

CREATE INDEX idx_ai_report_runs_conversation_finished
    ON ai_report_runs(conversation_id, finished_at DESC, id DESC);

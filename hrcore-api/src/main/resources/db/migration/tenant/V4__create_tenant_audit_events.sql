CREATE TABLE IF NOT EXISTS tenant_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_subject VARCHAR(150),
    actor_id UUID,
    actor_email VARCHAR(255),
    tenant_slug VARCHAR(100),
    event_type VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(100),
    event_details TEXT,
    ip_address INET,
    user_agent TEXT,
    request_id VARCHAR(100),
    correlation_id VARCHAR(100),
    source VARCHAR(50) NOT NULL DEFAULT 'APPLICATION',
    outcome VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    before_state JSONB,
    after_state JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_events_created_at
    ON tenant_audit_events(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_events_event_type
    ON tenant_audit_events(event_type);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_events_actor_id
    ON tenant_audit_events(actor_id);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_events_tenant_slug
    ON tenant_audit_events(tenant_slug);

CREATE INDEX IF NOT EXISTS idx_tenant_audit_events_request_id
    ON tenant_audit_events(request_id);

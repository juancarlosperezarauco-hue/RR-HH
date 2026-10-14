CREATE TABLE tenant_auth_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    token_family_id UUID NOT NULL,
    refresh_token_hash CHAR(64) NOT NULL UNIQUE,
    refresh_generation BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL,
    last_used_at TIMESTAMPTZ,
    last_rotated_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    user_agent TEXT,
    ip_address INET,
    CONSTRAINT fk_tenant_auth_sessions_user FOREIGN KEY (user_id)
        REFERENCES tenant_users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_tenant_auth_sessions_generation CHECK (refresh_generation > 0),
    CONSTRAINT ck_tenant_auth_sessions_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_tenant_auth_sessions_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_tenant_sessions_user ON tenant_auth_sessions(user_id);
CREATE INDEX idx_tenant_sessions_expires ON tenant_auth_sessions(expires_at);
CREATE INDEX idx_tenant_sessions_family ON tenant_auth_sessions(token_family_id);

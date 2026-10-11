CREATE TABLE IF NOT EXISTS tenant_account_activation_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(150) NOT NULL,
    token VARCHAR(255) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    used_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_account_activation_tokens_email
    ON tenant_account_activation_tokens(email);

CREATE INDEX IF NOT EXISTS idx_tenant_account_activation_tokens_expires_at
    ON tenant_account_activation_tokens(expires_at);

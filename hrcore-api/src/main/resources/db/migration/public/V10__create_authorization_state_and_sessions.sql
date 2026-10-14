CREATE TABLE public.authorization_catalog_state (
    id SMALLINT PRIMARY KEY,
    catalog_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_authorization_catalog_singleton CHECK (id = 1),
    CONSTRAINT ck_authorization_catalog_version CHECK (catalog_version > 0)
);

INSERT INTO public.authorization_catalog_state (id, catalog_version, updated_at)
VALUES (1, 1, NOW());

CREATE TABLE public.platform_auth_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    superadmin_id UUID NOT NULL,
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
    CONSTRAINT fk_platform_auth_sessions_user FOREIGN KEY (superadmin_id)
        REFERENCES public.platform_superadmins(id) ON DELETE RESTRICT,
    CONSTRAINT ck_platform_auth_sessions_generation CHECK (refresh_generation > 0),
    CONSTRAINT ck_platform_auth_sessions_expiration CHECK (expires_at > created_at),
    CONSTRAINT ck_platform_auth_sessions_revocation CHECK (revoked_at IS NULL OR revoked_at >= created_at)
);

CREATE INDEX idx_platform_sessions_superadmin ON public.platform_auth_sessions(superadmin_id);
CREATE INDEX idx_platform_sessions_expires ON public.platform_auth_sessions(expires_at);
CREATE INDEX idx_platform_sessions_family ON public.platform_auth_sessions(token_family_id);

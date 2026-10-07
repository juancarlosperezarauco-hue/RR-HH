CREATE TABLE IF NOT EXISTS tenant_user_face_login_profiles (
    user_id UUID PRIMARY KEY,
    face_token VARCHAR(255) NOT NULL,
    face_id VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_tenant_user_face_login_profiles_user
        FOREIGN KEY (user_id) REFERENCES tenant_users(id) ON DELETE CASCADE
);

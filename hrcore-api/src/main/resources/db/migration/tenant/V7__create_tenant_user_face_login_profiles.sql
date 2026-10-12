
-- ============================================================
-- V7__create_tenant_user_face_login_profiles.sql
-- HRCore - Perfiles de reconocimiento facial por tenant
-- ============================================================
-- Incluye:
-- 1. Informacion del reconocimiento facial
-- 2. Fotografias y metadatos del usuario
-- 3. URL de fotografia de perfil
-- 4. Estado del reconocimiento facial
-- 5. Registro de fechas de enrolamiento y actualizacion
-- ============================================================


-- ============================================================
-- 1. PERFILES DE RECONOCIMIENTO FACIAL
-- ============================================================

CREATE TABLE IF NOT EXISTS tenant_user_face_login_profiles (
    user_id UUID PRIMARY KEY,

    face_token VARCHAR(255) NULL,
    face_id VARCHAR(255) NULL,

    profile_photo_bytes BYTEA NULL,
    profile_photo_content_type VARCHAR(100) NULL,
    profile_photo_filename VARCHAR(255) NULL,
    profile_photo_url VARCHAR(512) NULL,

    enabled BOOLEAN NOT NULL DEFAULT TRUE,

    enrolled_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tenant_user_face_login_profiles_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users(id)
        ON DELETE CASCADE
);

ALTER TABLE tenant_user_face_login_profiles
    ADD COLUMN IF NOT EXISTS profile_photo_bytes BYTEA,
    ADD COLUMN IF NOT EXISTS profile_photo_content_type VARCHAR(100),
    ADD COLUMN IF NOT EXISTS profile_photo_filename VARCHAR(255);

ALTER TABLE tenant_user_face_login_profiles
    ADD COLUMN IF NOT EXISTS profile_photo_url VARCHAR(512);

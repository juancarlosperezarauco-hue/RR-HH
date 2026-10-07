ALTER TABLE tenant_user_face_login_profiles
    ALTER COLUMN face_token DROP NOT NULL,
    ALTER COLUMN face_id DROP NOT NULL;

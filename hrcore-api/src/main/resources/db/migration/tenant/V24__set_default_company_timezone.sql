INSERT INTO tenant_settings (setting_key, setting_value, created_at, updated_at)
VALUES ('company.timezone', 'America/La_Paz', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;

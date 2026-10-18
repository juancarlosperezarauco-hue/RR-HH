CREATE TABLE IF NOT EXISTS tenant_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    setting_key VARCHAR(100) NOT NULL UNIQUE,
    setting_value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- Configuración inicial de empresa. El país y la moneda requieren confirmación
-- explícita; no se inventan valores para ningún tenant.
INSERT INTO tenant_settings(setting_key, setting_value, created_at, updated_at) VALUES
 ('company.timezone', 'America/La_Paz', NOW(), NOW()),
 ('hr.setup_completed', 'false', NOW(), NOW()),
 ('attendance.employee_web_punch_enabled', 'true', NOW(), NOW()),
 ('attendance.geolocation_required', 'false', NOW(), NOW()),
 ('attendance.break_punch_required', 'false', NOW(), NOW()),
 ('attendance.daily_processing_enabled', 'true', NOW(), NOW()),
 ('attendance.adjustment_approval_required', 'true', NOW(), NOW()),
 ('leave.self_service_enabled', 'true', NOW(), NOW()),
 ('leave.supervisor_approval_enabled', 'true', NOW(), NOW())
ON CONFLICT (setting_key) DO NOTHING;

-- Secuencias empresariales instaladas desde el principio.

CREATE TABLE business_sequences (
    sequence_key VARCHAR(50) PRIMARY KEY,
    last_value BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_business_sequences_last_value CHECK (last_value >= 0)
);

INSERT INTO business_sequences(sequence_key, last_value) VALUES
 ('department', 0),
 ('position', 0),
 ('employee', 0),
 ('employment_contract', 0),
 ('employee_file', 0),
 ('work_shift', 0),
 ('employee_incident', 0);

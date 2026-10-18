-- ============================================================
-- V17__create_employee_incidents.sql
-- HRCore - Gestión de incidencias laborales
-- ============================================================
-- Incluye:
-- 1. Incidencias por empleado
-- 2. Memorándums
-- 3. Amonestaciones
-- 4. Clasificación y gravedad
-- 5. Acciones administrativas
-- 6. Evidencias documentales
-- 7. Historial de seguimiento
-- ============================================================


-- ============================================================
-- 1. INCIDENCIAS
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_incidents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    employment_period_id UUID NOT NULL,

    incident_number VARCHAR(80) NOT NULL,

    incident_type VARCHAR(40) NOT NULL,

    severity VARCHAR(20) NOT NULL DEFAULT 'LOW',

    title VARCHAR(200) NOT NULL,

    description TEXT NOT NULL,

    occurred_at TIMESTAMPTZ NOT NULL,

    reported_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    location VARCHAR(200) NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',

    reported_by UUID NULL,

    assigned_to UUID NULL,

    employee_response TEXT NULL,
    employee_response_at TIMESTAMPTZ NULL,

    resolution TEXT NULL,
    resolved_at TIMESTAMPTZ NULL,

    closed_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employee_incidents_number
        UNIQUE (incident_number),

    CONSTRAINT fk_employee_incidents_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_incidents_period_employee
        FOREIGN KEY (employment_period_id,employee_id)
        REFERENCES employment_periods(id,employee_id) ON DELETE RESTRICT,

    CONSTRAINT fk_employee_incidents_reported_by
        FOREIGN KEY (reported_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_employee_incidents_assigned_to
        FOREIGN KEY (assigned_to)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_incidents_type
        CHECK (incident_type IN (
            'MEMORANDUM',
            'WARNING',
            'DISCIPLINARY',
            'MISCONDUCT',
            'PERFORMANCE',
            'SAFETY',
            'RECOGNITION',
            'OTHER'
        )),

    CONSTRAINT chk_employee_incidents_severity
        CHECK (severity IN (
            'LOW',
            'MEDIUM',
            'HIGH',
            'CRITICAL'
        )),

    CONSTRAINT chk_employee_incidents_status
        CHECK (status IN (
            'OPEN',
            'UNDER_REVIEW',
            'RESOLVED',
            'CLOSED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_employee_incidents_employee
    ON employee_incidents(employee_id, occurred_at DESC);

CREATE INDEX IF NOT EXISTS idx_employee_incidents_status
    ON employee_incidents(status);

CREATE INDEX IF NOT EXISTS idx_employee_incidents_type
    ON employee_incidents(incident_type);

CREATE INDEX IF NOT EXISTS idx_employee_incidents_assigned_to
    ON employee_incidents(assigned_to);


-- ============================================================
-- 2. ACCIONES ADMINISTRATIVAS
-- ============================================================

CREATE TABLE IF NOT EXISTS incident_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    incident_id UUID NOT NULL,

    action_type VARCHAR(40) NOT NULL,

    description TEXT NOT NULL,

    action_date DATE NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    responsible_user_id UUID NULL,

    completed_at TIMESTAMPTZ NULL,

    notes TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_incident_actions_incident
        FOREIGN KEY (incident_id)
        REFERENCES employee_incidents(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_incident_actions_responsible
        FOREIGN KEY (responsible_user_id)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_incident_actions_type
        CHECK (action_type IN (
            'VERBAL_WARNING',
            'WRITTEN_WARNING',
            'MEMORANDUM',
            'TRAINING',
            'CORRECTIVE_ACTION',
            'DISCIPLINARY_ACTION',
            'FOLLOW_UP',
            'OTHER'
        )),

    CONSTRAINT chk_incident_actions_status
        CHECK (status IN (
            'PENDING',
            'IN_PROGRESS',
            'COMPLETED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_incident_actions_incident
    ON incident_actions(incident_id);

CREATE INDEX IF NOT EXISTS idx_incident_actions_status
    ON incident_actions(status);


-- ============================================================
-- 3. EVIDENCIAS Y DOCUMENTOS
-- ============================================================

CREATE TABLE IF NOT EXISTS incident_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    incident_id UUID NOT NULL,

    attachment_type VARCHAR(30) NOT NULL DEFAULT 'EVIDENCE',

    filename VARCHAR(255) NOT NULL,

    storage_key VARCHAR(500) NOT NULL,

    mime_type VARCHAR(150) NOT NULL,

    file_size BIGINT NOT NULL,

    description TEXT NULL,

    uploaded_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_incident_attachments_incident
        FOREIGN KEY (incident_id)
        REFERENCES employee_incidents(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_incident_attachments_user
        FOREIGN KEY (uploaded_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_incident_attachments_type
        CHECK (attachment_type IN (
            'EVIDENCE',
            'MEMORANDUM',
            'EMPLOYEE_RESPONSE',
            'RESOLUTION',
            'OTHER'
        )),

    CONSTRAINT chk_incident_attachments_size
        CHECK (file_size >= 0)
);

CREATE INDEX IF NOT EXISTS idx_incident_attachments_incident
    ON incident_attachments(incident_id);


-- ============================================================
-- 4. HISTORIAL DE INCIDENCIAS
-- ============================================================

CREATE TABLE IF NOT EXISTS incident_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    incident_id UUID NOT NULL,

    action VARCHAR(40) NOT NULL,

    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,

    details JSONB NULL,

    comment TEXT NULL,

    performed_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_incident_history_incident
        FOREIGN KEY (incident_id)
        REFERENCES employee_incidents(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_incident_history_user
        FOREIGN KEY (performed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_incident_history_action
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'ASSIGNED',
            'STATUS_CHANGED',
            'RESPONSE_ADDED',
            'EVIDENCE_ADDED',
            'ACTION_REGISTERED',
            'RESOLVED',
            'CLOSED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_incident_history_incident
    ON incident_history(incident_id, created_at DESC);




-- El instante del incidente se interpreta en la zona IANA de la empresa.
CREATE FUNCTION bind_incident_employment_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE company_zone TEXT; incident_day DATE; inferred_period UUID;
BEGIN
    SELECT setting_value INTO company_zone FROM tenant_settings
     WHERE setting_key='company.timezone';
    IF company_zone IS NULL OR BTRIM(company_zone)='' THEN
        RAISE EXCEPTION 'Company timezone is required for incidents' USING ERRCODE='23514';
    END IF;
    incident_day := (NEW.occurred_at AT TIME ZONE company_zone)::DATE;
    SELECT id INTO inferred_period FROM employment_periods
      WHERE employee_id=NEW.employee_id AND start_date <= incident_day
        AND (end_date IS NULL OR end_date >= incident_day);
    IF inferred_period IS NULL THEN
        RAISE EXCEPTION 'Incident date is outside employment periods' USING ERRCODE='23514';
    END IF;
    IF NEW.employment_period_id IS NOT NULL AND NEW.employment_period_id<>inferred_period THEN
        RAISE EXCEPTION 'Incident belongs to a different employment period' USING ERRCODE='23514';
    END IF;
    NEW.employment_period_id:=inferred_period;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_bind_incident_period
BEFORE INSERT OR UPDATE OF employee_id, employment_period_id, occurred_at ON employee_incidents
FOR EACH ROW EXECUTE FUNCTION bind_incident_employment_period();
CREATE INDEX idx_employee_incidents_period ON employee_incidents(employment_period_id);

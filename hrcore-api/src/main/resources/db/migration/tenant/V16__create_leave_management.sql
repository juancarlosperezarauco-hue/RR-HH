-- ============================================================
-- V16__create_leave_management.sql
-- HRCore - Ausencias, vacaciones y permisos
-- ============================================================
-- Incluye:
-- 1. Tipos de ausencia
-- 2. Vacaciones y licencias
-- 3. Saldos de días
-- 4. Solicitudes
-- 5. Aprobaciones y rechazos
-- 6. Historial de solicitudes
-- 7. Documentos justificativos
-- ============================================================


-- ============================================================
-- 1. TIPOS DE AUSENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_types (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,

    description TEXT NULL,

    paid BOOLEAN NOT NULL DEFAULT FALSE,

    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,

    requires_attachment BOOLEAN NOT NULL DEFAULT FALSE,

    deducts_balance BOOLEAN NOT NULL DEFAULT FALSE,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_leave_types_code
        UNIQUE (code),

    CONSTRAINT chk_leave_types_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        ))
);


-- ============================================================
-- 2. SALDOS DE AUSENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_balances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    employment_period_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,

    balance_year INTEGER NOT NULL,

    opening_days NUMERIC(8,2) NOT NULL DEFAULT 0,

    accrued_days NUMERIC(8,2) NOT NULL DEFAULT 0,

    used_days NUMERIC(8,2) NOT NULL DEFAULT 0,

    adjusted_days NUMERIC(8,2) NOT NULL DEFAULT 0,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_leave_balances_period_type_year
        UNIQUE (employment_period_id,leave_type_id,balance_year),
    CONSTRAINT fk_leave_balances_period_employee
        FOREIGN KEY (employment_period_id,employee_id)
        REFERENCES employment_periods(id,employee_id) ON DELETE RESTRICT,

    CONSTRAINT fk_leave_balances_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_leave_balances_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_leave_balances_year
        CHECK (balance_year BETWEEN 1900 AND 9999),

    CONSTRAINT chk_leave_balances_days
        CHECK (
            opening_days >= 0
            AND accrued_days >= 0
            AND used_days >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_leave_balances_employee
    ON leave_balances(employee_id);
CREATE INDEX idx_leave_balances_period ON leave_balances(employment_period_id);

-- Cuando se omite period_id, usa el último periodo abierto del empleado;
-- el backend deberá enviar el periodo explícitamente para ajustes históricos.
CREATE FUNCTION bind_leave_balance_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE inferred_period UUID;
BEGIN
    IF NEW.employment_period_id IS NULL THEN
        SELECT id INTO inferred_period FROM employment_periods
         WHERE employee_id=NEW.employee_id AND end_date IS NULL
         ORDER BY period_number DESC LIMIT 1;
        IF inferred_period IS NULL THEN
            RAISE EXCEPTION 'No open employment period for leave balance' USING ERRCODE='23514';
        END IF;
        NEW.employment_period_id:=inferred_period;
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_bind_leave_balance_period
BEFORE INSERT OR UPDATE OF employee_id, employment_period_id ON leave_balances
FOR EACH ROW EXECUTE FUNCTION bind_leave_balance_period();


-- ============================================================
-- 3. SOLICITUDES DE AUSENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    employment_period_id UUID NOT NULL,
    leave_type_id UUID NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NOT NULL,

    start_time TIME NULL,
    end_time TIME NULL,

    requested_days NUMERIC(8,2) NOT NULL,

    reason TEXT NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    requested_by UUID NULL,

    submitted_at TIMESTAMPTZ NULL,

    reviewed_by UUID NULL,
    reviewed_at TIMESTAMPTZ NULL,

    review_comment TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_leave_requests_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_leave_requests_period_employee
        FOREIGN KEY (employment_period_id,employee_id)
        REFERENCES employment_periods(id,employee_id) ON DELETE RESTRICT,

    CONSTRAINT fk_leave_requests_type
        FOREIGN KEY (leave_type_id)
        REFERENCES leave_types(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_leave_requests_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_leave_requests_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_leave_requests_dates
        CHECK (end_date >= start_date),

    CONSTRAINT chk_leave_requests_days
        CHECK (requested_days > 0),

    CONSTRAINT chk_leave_requests_time_range
        CHECK (
            (start_time IS NULL AND end_time IS NULL)
            OR
            (
                start_time IS NOT NULL
                AND end_time IS NOT NULL
                AND start_date = end_date
                AND end_time > start_time
            )
        ),

    CONSTRAINT chk_leave_requests_status
        CHECK (status IN (
            'DRAFT',
            'PENDING',
            'APPROVED',
            'REJECTED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_leave_requests_employee
    ON leave_requests(employee_id, start_date DESC);

CREATE INDEX IF NOT EXISTS idx_leave_requests_status
    ON leave_requests(status);

CREATE INDEX IF NOT EXISTS idx_leave_requests_dates
    ON leave_requests(start_date, end_date);
CREATE INDEX idx_leave_requests_period ON leave_requests(employment_period_id);
CREATE TRIGGER trg_bind_leave_request_period
BEFORE INSERT OR UPDATE OF employee_id, employment_period_id, start_date ON leave_requests
FOR EACH ROW EXECUTE FUNCTION bind_employee_employment_period('start_date');


-- ============================================================
-- 4. HISTORIAL DE SOLICITUDES
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_request_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    leave_request_id UUID NOT NULL,

    previous_status VARCHAR(20) NULL,
    new_status VARCHAR(20) NOT NULL,

    comment TEXT NULL,

    performed_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_leave_history_request
        FOREIGN KEY (leave_request_id)
        REFERENCES leave_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_leave_history_user
        FOREIGN KEY (performed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_leave_history_new_status
        CHECK (new_status IN (
            'DRAFT',
            'PENDING',
            'APPROVED',
            'REJECTED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_leave_history_request
    ON leave_request_history(leave_request_id, created_at DESC);


-- ============================================================
-- 5. JUSTIFICATIVOS
-- ============================================================

CREATE TABLE IF NOT EXISTS leave_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    leave_request_id UUID NOT NULL,

    filename VARCHAR(255) NOT NULL,

    storage_key VARCHAR(500) NOT NULL,

    mime_type VARCHAR(150) NOT NULL,

    file_size BIGINT NOT NULL,

    uploaded_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_leave_attachments_request
        FOREIGN KEY (leave_request_id)
        REFERENCES leave_requests(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_leave_attachments_user
        FOREIGN KEY (uploaded_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_leave_attachments_size
        CHECK (file_size >= 0)
);

CREATE INDEX IF NOT EXISTS idx_leave_attachments_request
    ON leave_attachments(leave_request_id);




-- Catálogo inicial de ausencias reubicado desde V23.
INSERT INTO leave_types (
    id, code, name, description, paid,
    requires_approval, requires_attachment, deducts_balance, status
) VALUES
    (gen_random_uuid(), 'LEAVE_VACATION', 'Vacación anual',
     'Descanso anual sujeto al saldo disponible del empleado.', TRUE, TRUE, FALSE, TRUE, 'ACTIVE'),
    (gen_random_uuid(), 'MEDICAL_LEAVE', 'Baja médica',
     'Ausencia por enfermedad o incapacidad respaldada por certificado.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'PERSONAL_PERMISSION', 'Permiso personal',
     'Permiso solicitado por motivos personales.', FALSE, TRUE, FALSE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'MATERNITY_LEAVE', 'Licencia de maternidad',
     'Licencia remunerada por maternidad con respaldo documental.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'PATERNITY_LEAVE', 'Licencia de paternidad',
     'Licencia remunerada por paternidad con respaldo documental.', TRUE, TRUE, TRUE, FALSE, 'ACTIVE'),
    (gen_random_uuid(), 'UNPAID_LEAVE', 'Licencia sin goce de haberes',
     'Ausencia autorizada sin remuneración.', FALSE, TRUE, FALSE, FALSE, 'ACTIVE')
ON CONFLICT (code) DO NOTHING;


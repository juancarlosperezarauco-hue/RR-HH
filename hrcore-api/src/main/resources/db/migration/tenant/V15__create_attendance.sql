-- ============================================================
-- V15__create_attendance.sql
-- HRCore - Gestión de asistencia y marcaciones
-- ============================================================
-- Incluye:
-- 1. Registros diarios
-- 2. Marcaciones de entrada y salida
-- 3. Resultados calculados
-- 4. Tardanzas y salidas anticipadas
-- 5. Ajustes de asistencia
-- 6. Horas extraordinarias
-- ============================================================


-- ============================================================
-- 1. ASISTENCIA DIARIA
-- ============================================================

CREATE TABLE IF NOT EXISTS attendance_records (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,

    shift_assignment_id UUID NULL,

    work_date DATE NOT NULL,

    expected_start_at TIMESTAMPTZ NULL,
    expected_end_at TIMESTAMPTZ NULL,

    actual_first_in_at TIMESTAMPTZ NULL,
    actual_last_out_at TIMESTAMPTZ NULL,

    worked_minutes INTEGER NOT NULL DEFAULT 0,

    late_minutes INTEGER NOT NULL DEFAULT 0,
    early_departure_minutes INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',

    calculation_details JSONB NULL,

    calculated_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_attendance_employee_date
        UNIQUE (employee_id, work_date),

    CONSTRAINT fk_attendance_records_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_records_shift
        FOREIGN KEY (shift_assignment_id)
        REFERENCES employee_shift_assignments(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_attendance_records_status
        CHECK (status IN (
            'PENDING',
            'PRESENT',
            'LATE',
            'ABSENT',
            'JUSTIFIED',
            'INCOMPLETE',
            'DAY_OFF'
        )),

    CONSTRAINT chk_attendance_minutes
        CHECK (
            worked_minutes >= 0
            AND late_minutes >= 0
            AND early_departure_minutes >= 0
        ),

    CONSTRAINT chk_attendance_actual_times
        CHECK (
            actual_first_in_at IS NULL
            OR actual_last_out_at IS NULL
            OR actual_last_out_at >= actual_first_in_at
        )
);

CREATE INDEX IF NOT EXISTS idx_attendance_records_date
    ON attendance_records(work_date);

CREATE INDEX IF NOT EXISTS idx_attendance_records_status
    ON attendance_records(status);


-- ============================================================
-- 2. MARCACIONES
-- ============================================================

CREATE TABLE IF NOT EXISTS attendance_punches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,

    attendance_record_id UUID NULL,

    punch_type VARCHAR(20) NOT NULL,

    punched_at TIMESTAMPTZ NOT NULL,

    source VARCHAR(30) NOT NULL,

    device_identifier VARCHAR(150) NULL,

    latitude NUMERIC(10,7) NULL,
    longitude NUMERIC(10,7) NULL,

    recorded_by UUID NULL,

    notes TEXT NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_attendance_punches_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_punches_record
        FOREIGN KEY (attendance_record_id)
        REFERENCES attendance_records(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_punches_user
        FOREIGN KEY (recorded_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_attendance_punch_type
        CHECK (punch_type IN (
            'IN',
            'OUT',
            'BREAK_START',
            'BREAK_END'
        )),

    CONSTRAINT chk_attendance_punch_source
        CHECK (source IN (
            'WEB',
            'MOBILE',
            'BIOMETRIC',
            'DEVICE',
            'MANUAL',
            'API'
        )),

    CONSTRAINT chk_attendance_latitude
        CHECK (
            latitude IS NULL
            OR latitude BETWEEN -90 AND 90
        ),

    CONSTRAINT chk_attendance_longitude
        CHECK (
            longitude IS NULL
            OR longitude BETWEEN -180 AND 180
        )
);

CREATE INDEX IF NOT EXISTS idx_attendance_punches_employee
    ON attendance_punches(employee_id, punched_at DESC);

CREATE INDEX IF NOT EXISTS idx_attendance_punches_record
    ON attendance_punches(attendance_record_id);


-- ============================================================
-- 3. AJUSTES DE ASISTENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS attendance_adjustments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    attendance_record_id UUID NOT NULL,

    adjustment_type VARCHAR(30) NOT NULL,

    reason TEXT NOT NULL,

    previous_values JSONB NULL,
    requested_values JSONB NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    requested_by UUID NOT NULL,

    reviewed_by UUID NULL,
    reviewed_at TIMESTAMPTZ NULL,

    review_comment TEXT NULL,

    applied_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_attendance_adjustments_record
        FOREIGN KEY (attendance_record_id)
        REFERENCES attendance_records(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_adjustments_requested_by
        FOREIGN KEY (requested_by)
        REFERENCES tenant_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_adjustments_reviewed_by
        FOREIGN KEY (reviewed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_attendance_adjustment_type
        CHECK (adjustment_type IN (
            'MISSING_PUNCH',
            'TIME_CORRECTION',
            'STATUS_CORRECTION',
            'OTHER'
        )),

    CONSTRAINT chk_attendance_adjustment_status
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_attendance_adjustments_record
    ON attendance_adjustments(attendance_record_id);


-- ============================================================
-- 4. HORAS EXTRAORDINARIAS
-- ============================================================

CREATE TABLE IF NOT EXISTS attendance_overtime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    attendance_record_id UUID NOT NULL,

    requested_minutes INTEGER NOT NULL,

    approved_minutes INTEGER NULL,

    reason TEXT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    approved_by UUID NULL,
    approved_at TIMESTAMPTZ NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_attendance_overtime_record
        FOREIGN KEY (attendance_record_id)
        REFERENCES attendance_records(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_overtime_approved_by
        FOREIGN KEY (approved_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_attendance_overtime_minutes
        CHECK (
            requested_minutes > 0
            AND (
                approved_minutes IS NULL
                OR (
                    approved_minutes >= 0
                    AND approved_minutes <= requested_minutes
                )
            )
        ),

    CONSTRAINT chk_attendance_overtime_status
        CHECK (status IN (
            'PENDING',
            'APPROVED',
            'REJECTED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_attendance_overtime_record
    ON attendance_overtime(attendance_record_id);



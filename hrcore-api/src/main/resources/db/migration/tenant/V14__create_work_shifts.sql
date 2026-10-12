-- ============================================================
-- V14__create_work_shifts.sql
-- HRCore - Gestión de turnos y jornadas laborales
-- ============================================================
-- Incluye:
-- 1. Definición de turnos
-- 2. Jornadas diurnas y nocturnas
-- 3. Tolerancias
-- 4. Descansos
-- 5. Asignaciones por empleado
-- 6. Excepciones de calendario
-- ============================================================


-- ============================================================
-- 1. TURNOS
-- ============================================================

CREATE TABLE IF NOT EXISTS work_shifts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,

    description TEXT NULL,

    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    crosses_midnight BOOLEAN NOT NULL DEFAULT FALSE,

    late_tolerance_minutes INTEGER NOT NULL DEFAULT 0,
    early_departure_tolerance_minutes INTEGER NOT NULL DEFAULT 0,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_work_shifts_code
        UNIQUE (code),

    CONSTRAINT fk_work_shifts_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_work_shifts_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        )),

    CONSTRAINT chk_work_shifts_tolerance
        CHECK (
            late_tolerance_minutes >= 0
            AND early_departure_tolerance_minutes >= 0
        ),

    CONSTRAINT chk_work_shifts_midnight
        CHECK (
            (crosses_midnight = FALSE AND end_time > start_time)
            OR
            (crosses_midnight = TRUE AND end_time <= start_time)
        )
);


-- ============================================================
-- 2. DESCANSOS
-- ============================================================

CREATE TABLE IF NOT EXISTS shift_breaks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    shift_id UUID NOT NULL,

    name VARCHAR(100) NOT NULL,

    start_time TIME NOT NULL,
    end_time TIME NOT NULL,

    duration_minutes INTEGER NOT NULL,

    paid BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_shift_breaks_shift
        FOREIGN KEY (shift_id)
        REFERENCES work_shifts(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_shift_break_duration
        CHECK (
            duration_minutes > 0
            AND duration_minutes <= 1440
        )
);

CREATE INDEX IF NOT EXISTS idx_shift_breaks_shift
    ON shift_breaks(shift_id);


-- ============================================================
-- 3. ASIGNACIONES DE TURNOS
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_shift_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    shift_id UUID NOT NULL,

    start_date DATE NOT NULL,
    end_date DATE NULL,

    -- Arreglo de días ISO: 1=lunes, 7=domingo.
    -- La validez y unicidad del contenido se valida en servicio.
    weekdays SMALLINT[] NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_employee_shift_assignments_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_shift_assignments_shift
        FOREIGN KEY (shift_id)
        REFERENCES work_shifts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_shift_assignments_user
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_shift_assignments_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        ),

    CONSTRAINT chk_employee_shift_assignments_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE',
            'CANCELLED'
        )),

    CONSTRAINT chk_employee_shift_weekdays_not_empty
        CHECK (CARDINALITY(weekdays) BETWEEN 1 AND 7),

    CONSTRAINT chk_employee_shift_weekdays_range
        CHECK (weekdays <@ ARRAY[1,2,3,4,5,6,7]::SMALLINT[])
);

CREATE INDEX IF NOT EXISTS idx_employee_shift_assignments_employee
    ON employee_shift_assignments(employee_id, start_date DESC);

CREATE INDEX IF NOT EXISTS idx_employee_shift_assignments_shift
    ON employee_shift_assignments(shift_id);


-- ============================================================
-- 4. EXCEPCIONES DE CALENDARIO
-- ============================================================

CREATE TABLE IF NOT EXISTS work_calendar_exceptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NULL,
    department_id UUID NULL,

    exception_date DATE NOT NULL,

    exception_type VARCHAR(30) NOT NULL,

    description TEXT NULL,

    work_required BOOLEAN NOT NULL DEFAULT FALSE,

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_calendar_exceptions_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_calendar_exceptions_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_calendar_exceptions_user
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_calendar_exceptions_type
        CHECK (exception_type IN (
            'HOLIDAY',
            'NON_WORKING_DAY',
            'SPECIAL_WORKING_DAY',
            'SCHEDULE_CHANGE',
            'OTHER'
        )),

    CONSTRAINT chk_calendar_exception_scope
        CHECK (
            NOT (
                employee_id IS NOT NULL
                AND department_id IS NOT NULL
            )
        )
);

CREATE INDEX IF NOT EXISTS idx_calendar_exceptions_date
    ON work_calendar_exceptions(exception_date);

CREATE INDEX IF NOT EXISTS idx_calendar_exceptions_employee
    ON work_calendar_exceptions(employee_id);

CREATE INDEX IF NOT EXISTS idx_calendar_exceptions_department
    ON work_calendar_exceptions(department_id);


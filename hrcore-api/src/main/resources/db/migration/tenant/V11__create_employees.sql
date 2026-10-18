-- ============================================================
-- V11__create_employees.sql
-- HRCore - Gestión integral de empleados
-- ============================================================
-- Incluye:
-- 1. Ficha principal de empleados
-- 2. Vinculación opcional con usuarios
-- 3. Asignaciones laborales
-- 4. Historial de departamentos y cargos
-- 5. Contactos de emergencia
-- 6. Responsable de departamentos
-- ============================================================


-- ============================================================
-- 1. EMPLEADOS
-- ============================================================

CREATE TABLE IF NOT EXISTS employees (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    tenant_user_id UUID NULL,

    employee_number VARCHAR(50) NOT NULL,

    first_name VARCHAR(100) NOT NULL,
    middle_name VARCHAR(100),
    last_name VARCHAR(100) NOT NULL,
    second_last_name VARCHAR(100),

    document_type VARCHAR(30) NOT NULL,
    document_number VARCHAR(50) NOT NULL,

    birth_date DATE NULL,

    gender VARCHAR(30) NULL,
    nationality VARCHAR(80) NULL,

    personal_email VARCHAR(150) NULL,
    work_email VARCHAR(150) NULL,

    phone VARCHAR(40) NULL,
    mobile_phone VARCHAR(40) NULL,

    address TEXT NULL,

    hire_date DATE NOT NULL,
    termination_date DATE NULL,

    employment_status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,
    updated_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employees_number
        UNIQUE (employee_number),

    CONSTRAINT uq_employees_document
        UNIQUE (document_type, document_number),

    CONSTRAINT uq_employees_tenant_user
        UNIQUE (tenant_user_id),

    CONSTRAINT fk_employees_tenant_user
        FOREIGN KEY (tenant_user_id)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_employees_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_employees_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employees_status
        CHECK (employment_status IN (
            'ACTIVE',
            'ON_LEAVE',
            'SUSPENDED',
            'TERMINATED',
            'INACTIVE'
        )),

    CONSTRAINT chk_employees_dates CHECK (
        (termination_date IS NULL OR termination_date >= hire_date)
        AND ((employment_status = 'TERMINATED') = (termination_date IS NOT NULL))
    ),

    CONSTRAINT chk_employees_first_name
        CHECK (BTRIM(first_name) <> ''),

    CONSTRAINT chk_employees_last_name
        CHECK (BTRIM(last_name) <> '')
);

CREATE INDEX IF NOT EXISTS idx_employees_status
    ON employees(employment_status);

CREATE INDEX IF NOT EXISTS idx_employees_hire_date
    ON employees(hire_date);

CREATE INDEX IF NOT EXISTS idx_employees_last_name
    ON employees(last_name, first_name);

-- hire_date/termination_date/employment_status son una proyección del ÚLTIMO
-- período para conservar compatibilidad con los DTO existentes. El historial
-- real y la identidad de cada reingreso residen en employment_periods.
COMMENT ON COLUMN employees.hire_date IS 'Fecha inicial del periodo laboral mas reciente (snapshot); historial en employment_periods';
COMMENT ON COLUMN employees.termination_date IS 'Fecha final del periodo mas reciente, nula si esta abierto (snapshot)';
COMMENT ON COLUMN employees.employment_status IS 'Estado del periodo mas reciente (snapshot)';

-- Evita solapes tanto en periodos laborales como asignaciones/contratos.
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

CREATE TABLE employment_periods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    period_number INTEGER NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_employment_period_employee_number UNIQUE (employee_id, period_number),
    CONSTRAINT uq_employment_period_id_employee UNIQUE (id, employee_id),
    CONSTRAINT chk_employment_period_number CHECK (period_number >= 1),
    CONSTRAINT chk_employment_period_status CHECK (status IN ('ACTIVE','ON_LEAVE','SUSPENDED','INACTIVE','TERMINATED')),
    CONSTRAINT chk_employment_period_dates CHECK (
        (status = 'TERMINATED' AND end_date IS NOT NULL AND end_date >= start_date)
        OR (status <> 'TERMINATED' AND end_date IS NULL)
    ),
    CONSTRAINT ex_employment_period_no_overlap EXCLUDE USING gist (
        employee_id WITH =,
        daterange(start_date, COALESCE(end_date, 'infinity'::DATE), '[]') WITH &&
    ) DEFERRABLE INITIALLY IMMEDIATE
);
CREATE INDEX idx_employment_period_employee ON employment_periods(employee_id, start_date DESC);
CREATE UNIQUE INDEX uq_employment_period_open ON employment_periods(employee_id)
    WHERE end_date IS NULL;

CREATE FUNCTION prepare_employment_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE next_number INTEGER;
BEGIN
    -- Serializa reingresos simultaneos de la misma persona.
    PERFORM 1 FROM employees WHERE id = NEW.employee_id FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Employee not found' USING ERRCODE = '23503'; END IF;
    SELECT COALESCE(MAX(period_number),0)+1 INTO next_number
      FROM employment_periods WHERE employee_id = NEW.employee_id;
    IF NEW.period_number IS NOT NULL AND NEW.period_number <> next_number THEN
        RAISE EXCEPTION 'Employment period number must be sequential' USING ERRCODE='23514';
    END IF;
    NEW.period_number := next_number;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_prepare_employment_period
BEFORE INSERT ON employment_periods
FOR EACH ROW EXECUTE FUNCTION prepare_employment_period();

-- Cuando el backend histórico escribe la ficha del empleado, se conserva
-- el comportamiento y se sincroniza el período correspondiente.
CREATE FUNCTION sync_period_from_employee()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE last_period RECORD;
BEGIN
    IF TG_OP = 'INSERT' THEN
        INSERT INTO employment_periods(employee_id, start_date, end_date, status)
        VALUES (NEW.id, NEW.hire_date, NEW.termination_date, NEW.employment_status);
        RETURN NEW;
    END IF;
    SELECT id, status INTO last_period FROM employment_periods
      WHERE employee_id=NEW.id ORDER BY period_number DESC LIMIT 1 FOR UPDATE;
    IF NOT FOUND THEN RAISE EXCEPTION 'Employee has no employment period' USING ERRCODE='23514'; END IF;
    -- La reactivacion directa de un periodo terminado no es un reingreso.
    IF OLD.employment_status = 'TERMINATED' AND NEW.employment_status <> 'TERMINATED'
       AND last_period.status = 'TERMINATED' THEN
        RAISE EXCEPTION 'Create a new employment period for rehire' USING ERRCODE='23514';
    END IF;
    UPDATE employment_periods p
       SET start_date=NEW.hire_date, end_date=NEW.termination_date,
           status=NEW.employment_status, updated_at=NOW()
     WHERE p.id=last_period.id AND
       (p.start_date, p.end_date, p.status) IS DISTINCT FROM
       (NEW.hire_date, NEW.termination_date, NEW.employment_status);
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_sync_period_from_employee_insert
AFTER INSERT ON employees FOR EACH ROW EXECUTE FUNCTION sync_period_from_employee();
CREATE TRIGGER trg_sync_period_from_employee_update
AFTER UPDATE OF hire_date, termination_date, employment_status ON employees
FOR EACH ROW EXECUTE FUNCTION sync_period_from_employee();

-- Reingresar consiste en insertar otro employment_periods (nunca otro
-- employees), después de terminar el anterior. Actualiza el snapshot.
CREATE FUNCTION sync_employee_from_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.period_number = (
        SELECT MAX(period_number) FROM employment_periods WHERE employee_id = NEW.employee_id
    ) THEN
        UPDATE employees e SET
            hire_date=NEW.start_date,
            termination_date=NEW.end_date,
            employment_status=NEW.status,
            updated_at=NOW()
        WHERE e.id=NEW.employee_id
          AND (e.hire_date,e.termination_date,e.employment_status)
              IS DISTINCT FROM (NEW.start_date,NEW.end_date,NEW.status);
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_sync_employee_from_period
AFTER INSERT OR UPDATE OF start_date, end_date, status ON employment_periods
FOR EACH ROW EXECUTE FUNCTION sync_employee_from_period();

-- Asocia filas operativas a un período por su fecha, sin depender de que
-- todos los DTO Java incorporen inmediatamente employmentPeriodId.
CREATE FUNCTION bind_employee_employment_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE effective_date DATE; period_key UUID;
BEGIN
    effective_date := (to_jsonb(NEW) ->> TG_ARGV[0])::DATE;
    IF effective_date IS NULL THEN
        RAISE EXCEPTION 'Employment date is required' USING ERRCODE='23514';
    END IF;
    SELECT p.id INTO period_key FROM employment_periods p
     WHERE p.employee_id=NEW.employee_id
       AND p.start_date <= effective_date
       AND (p.end_date IS NULL OR p.end_date >= effective_date);
    IF period_key IS NULL THEN
        RAISE EXCEPTION 'No employment period contains the operation date' USING ERRCODE='23514';
    END IF;
    IF NEW.employment_period_id IS NOT NULL AND NEW.employment_period_id <> period_key THEN
        RAISE EXCEPTION 'Employment period does not match employee and date' USING ERRCODE='23514';
    END IF;
    NEW.employment_period_id := period_key;
    RETURN NEW;
END; $$;



-- ============================================================
-- 2. ASIGNACIONES LABORALES
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    employment_period_id UUID NOT NULL,
    department_id UUID NOT NULL,
    position_id UUID NOT NULL,

    supervisor_employee_id UUID NULL,

    start_date DATE NOT NULL,
    end_date DATE NULL,

    assignment_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',
    assignment_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    reason VARCHAR(255),

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_employee_assignments_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_employee_assignments_id_employee UNIQUE(id,employee_id),
    CONSTRAINT fk_employee_assignments_period FOREIGN KEY (employment_period_id,employee_id)
        REFERENCES employment_periods(id,employee_id) ON DELETE RESTRICT,

    CONSTRAINT fk_employee_assignments_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_assignments_position
        FOREIGN KEY (position_id)
        REFERENCES positions(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_assignments_supervisor
        FOREIGN KEY (supervisor_employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_assignments_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_assignments_type
        CHECK (assignment_type IN (
            'PRIMARY',
            'TEMPORARY',
            'ADDITIONAL'
        )),

    CONSTRAINT chk_employee_assignments_status CHECK (assignment_status IN ('ACTIVE','CANCELLED')),

    CONSTRAINT chk_employee_assignments_dates
        CHECK (
            end_date IS NULL
            OR end_date >= start_date
        ),

    CONSTRAINT chk_employee_assignments_supervisor
        CHECK (
            supervisor_employee_id IS NULL
            OR supervisor_employee_id <> employee_id
        )
);

CREATE INDEX IF NOT EXISTS idx_employee_assignments_employee
    ON employee_assignments(employee_id, start_date DESC);

CREATE INDEX IF NOT EXISTS idx_employee_assignments_department
    ON employee_assignments(department_id);

CREATE INDEX IF NOT EXISTS idx_employee_assignments_position
    ON employee_assignments(position_id);

-- El índice anterior solo protegía asignaciones abiertas; ahora ningún rango
-- PRIMARY ACTIVE puede solaparse, aunque ambos tengan fecha de fin.
ALTER TABLE employee_assignments
    ADD CONSTRAINT ex_employee_primary_assignment_no_overlap EXCLUDE USING gist (
        employee_id WITH =,
        daterange(start_date, COALESCE(end_date,'infinity'::DATE), '[]') WITH &&
    ) WHERE (assignment_type='PRIMARY' AND assignment_status='ACTIVE')
      DEFERRABLE INITIALLY IMMEDIATE;
CREATE INDEX idx_employee_assignments_active_primary
    ON employee_assignments(employee_id,start_date)
    WHERE assignment_type='PRIMARY' AND assignment_status='ACTIVE';
CREATE INDEX idx_employee_assignments_period ON employee_assignments(employment_period_id);
CREATE TRIGGER trg_bind_employee_assignment_period
BEFORE INSERT OR UPDATE OF employee_id, employment_period_id, start_date ON employee_assignments
FOR EACH ROW EXECUTE FUNCTION bind_employee_employment_period('start_date');


-- ============================================================
-- 3. CONTACTOS DE EMERGENCIA
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_emergency_contacts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,

    full_name VARCHAR(200) NOT NULL,
    relationship VARCHAR(80),

    phone VARCHAR(40) NOT NULL,
    alternative_phone VARCHAR(40),

    address TEXT,

    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_employee_emergency_contacts_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT
);

CREATE INDEX IF NOT EXISTS idx_employee_emergency_contacts_employee
    ON employee_emergency_contacts(employee_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_primary_emergency_contact
    ON employee_emergency_contacts(employee_id)
    WHERE is_primary = TRUE;


-- ============================================================
-- 4. RELACIÓN RESPONSABLE - DEPARTAMENTO
-- ============================================================

ALTER TABLE departments
    ADD CONSTRAINT fk_departments_manager_employee
    FOREIGN KEY (manager_employee_id)
    REFERENCES employees(id)
    ON DELETE SET NULL;




-- El titular de una asignación es inmutable para conservar su trazabilidad.
CREATE FUNCTION protect_employee_assignment_owner()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.employee_id IS DISTINCT FROM OLD.employee_id THEN
        RAISE EXCEPTION 'Employee assignment owner is immutable' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_protect_employee_assignment_owner
BEFORE UPDATE OF employee_id ON employee_assignments
FOR EACH ROW EXECUTE FUNCTION protect_employee_assignment_owner();


-- Un mismo empleado puede tener distintas terminaciones, una por período.
CREATE TABLE employee_terminations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employees(id) ON DELETE RESTRICT,
    employment_period_id UUID NOT NULL UNIQUE,
    effective_date DATE NOT NULL,
    reason TEXT NOT NULL,
    performed_by UUID NULL REFERENCES tenant_users(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_employee_terminations_period_employee
        FOREIGN KEY (employment_period_id,employee_id)
        REFERENCES employment_periods(id,employee_id) ON DELETE RESTRICT,
    CONSTRAINT chk_employee_terminations_reason CHECK (BTRIM(reason) <> '')
);
CREATE INDEX idx_employee_terminations_employee ON employee_terminations(employee_id,effective_date DESC);

CREATE FUNCTION bind_termination_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE period_key UUID;
BEGIN
    SELECT p.id INTO period_key FROM employment_periods p
     WHERE p.employee_id=NEW.employee_id
       AND p.status='TERMINATED' AND p.end_date=NEW.effective_date;
    IF period_key IS NULL THEN
        RAISE EXCEPTION 'A terminated employment period is required' USING ERRCODE='23514';
    END IF;
    IF NEW.employment_period_id IS NOT NULL AND NEW.employment_period_id <> period_key THEN
        RAISE EXCEPTION 'Termination belongs to a different employment period' USING ERRCODE='23514';
    END IF;
    NEW.employment_period_id := period_key;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_bind_termination_period
BEFORE INSERT OR UPDATE OF employee_id, employment_period_id, effective_date ON employee_terminations
FOR EACH ROW EXECUTE FUNCTION bind_termination_period();

-- Los períodos terminados son históricos e inmutables. Un reingreso
-- debe crear un registro nuevo, nunca reabrir uno terminado.
CREATE FUNCTION protect_terminated_employment_period()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.status='TERMINATED' AND
       (NEW.status,NEW.start_date,NEW.end_date) IS DISTINCT FROM
       (OLD.status,OLD.start_date,OLD.end_date) THEN
        RAISE EXCEPTION 'Terminated employment period cannot be reopened or redated'
            USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_protect_terminated_employment_period
BEFORE UPDATE OF status, start_date, end_date ON employment_periods
FOR EACH ROW EXECUTE FUNCTION protect_terminated_employment_period();

-- Al confirmar la transacción, cada período terminado debe tener su evento
-- histórico. Es diferido para permitir que el caso de uso haga ambas escrituras.
CREATE FUNCTION assert_terminated_period_has_event()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.status='TERMINATED' AND NOT EXISTS (
        SELECT 1 FROM employee_terminations t WHERE t.employment_period_id=NEW.id
    ) THEN
        RAISE EXCEPTION 'Terminated employment period must have a termination event'
            USING ERRCODE='23514';
    END IF;
    RETURN NULL;
END; $$;
CREATE CONSTRAINT TRIGGER trg_assert_terminated_period_has_event
AFTER INSERT OR UPDATE OF status, end_date ON employment_periods
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW EXECUTE FUNCTION assert_terminated_period_has_event();


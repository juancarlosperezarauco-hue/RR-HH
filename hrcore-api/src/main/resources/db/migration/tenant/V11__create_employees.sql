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

    CONSTRAINT chk_employees_dates
        CHECK (
            termination_date IS NULL
            OR termination_date >= hire_date
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


-- ============================================================
-- 2. ASIGNACIONES LABORALES
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,
    department_id UUID NOT NULL,
    position_id UUID NOT NULL,

    supervisor_employee_id UUID NULL,

    start_date DATE NOT NULL,
    end_date DATE NULL,

    assignment_type VARCHAR(30) NOT NULL DEFAULT 'PRIMARY',

    reason VARCHAR(255),

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_employee_assignments_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

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

CREATE UNIQUE INDEX IF NOT EXISTS uq_employee_current_primary_assignment
    ON employee_assignments(employee_id)
    WHERE assignment_type = 'PRIMARY'
      AND end_date IS NULL;


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


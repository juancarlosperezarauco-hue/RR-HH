-- ============================================================
-- V9__create_departments.sql
-- HRCore - Gestión organizacional de departamentos
-- ============================================================
-- Incluye:
-- 1. Registro de departamentos
-- 2. Jerarquías organizacionales
-- 3. Estados de departamentos
-- 4. Responsables organizacionales
-- 5. Historial de modificaciones
-- ============================================================


-- ============================================================
-- 1. DEPARTAMENTOS
-- ============================================================

CREATE TABLE IF NOT EXISTS departments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,

    parent_department_id UUID NULL,

    manager_employee_id UUID NULL,

    cost_center_code VARCHAR(50),

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,
    updated_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_departments_code
        UNIQUE (code),

    CONSTRAINT fk_departments_parent
        FOREIGN KEY (parent_department_id)
        REFERENCES departments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_departments_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_departments_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_departments_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        )),

    CONSTRAINT chk_departments_no_self_parent
        CHECK (
            parent_department_id IS NULL
            OR parent_department_id <> id
        ),

    CONSTRAINT chk_departments_code_not_blank
        CHECK (BTRIM(code) <> ''),

    CONSTRAINT chk_departments_name_not_blank
        CHECK (BTRIM(name) <> '')
);

CREATE INDEX IF NOT EXISTS idx_departments_parent
    ON departments(parent_department_id);

CREATE INDEX IF NOT EXISTS idx_departments_status
    ON departments(status);

CREATE INDEX IF NOT EXISTS idx_departments_manager
    ON departments(manager_employee_id);


-- ============================================================
-- 2. HISTORIAL DE DEPARTAMENTOS
-- ============================================================

CREATE TABLE IF NOT EXISTS department_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    department_id UUID NOT NULL,

    action VARCHAR(30) NOT NULL,

    previous_data JSONB NULL,
    new_data JSONB NULL,

    reason TEXT,

    performed_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_department_history_department
        FOREIGN KEY (department_id)
        REFERENCES departments(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_department_history_user
        FOREIGN KEY (performed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_department_history_action
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'ACTIVATED',
            'DEACTIVATED',
            'REORGANIZED',
            'MANAGER_CHANGED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_department_history_department
    ON department_history(department_id, created_at DESC);


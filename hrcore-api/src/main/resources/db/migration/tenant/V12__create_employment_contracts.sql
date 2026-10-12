-- ============================================================
-- V12__create_employment_contracts.sql
-- HRCore - Gestión de contratos laborales
-- ============================================================
-- Incluye:
-- 1. Contratos laborales
-- 2. Tipos de contratación
-- 3. Condiciones económicas
-- 4. Formalización y terminación
-- 5. Adendas contractuales
-- 6. Historial de eventos
-- ============================================================


-- ============================================================
-- 1. CONTRATOS LABORALES
-- ============================================================

CREATE TABLE IF NOT EXISTS employment_contracts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,

    contract_number VARCHAR(80) NOT NULL,

    contract_type VARCHAR(30) NOT NULL,

    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',

    start_date DATE NOT NULL,
    end_date DATE NULL,

    signed_at TIMESTAMPTZ NULL,

    base_salary NUMERIC(19,4) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'BOB',

    payment_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',

    weekly_hours NUMERIC(6,2) NULL,

    probation_end_date DATE NULL,

    termination_date DATE NULL,
    termination_reason TEXT NULL,

    terms TEXT NULL,

    created_by UUID NULL,
    updated_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employment_contracts_number
        UNIQUE (contract_number),

    CONSTRAINT fk_employment_contracts_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employment_contracts_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_employment_contracts_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employment_contracts_type
        CHECK (contract_type IN (
            'INDEFINITE',
            'FIXED_TERM',
            'PART_TIME',
            'TEMPORARY',
            'INTERNSHIP',
            'OTHER'
        )),

    CONSTRAINT chk_employment_contracts_status
        CHECK (status IN (
            'DRAFT',
            'PENDING_SIGNATURE',
            'ACTIVE',
            'EXPIRED',
            'TERMINATED',
            'CANCELLED'
        )),

    CONSTRAINT chk_employment_contracts_salary
        CHECK (base_salary >= 0),

    CONSTRAINT chk_employment_contracts_currency
        CHECK (currency ~ '^[A-Z]{3}$'),

    CONSTRAINT chk_employment_contracts_frequency
        CHECK (payment_frequency IN (
            'MONTHLY',
            'BIWEEKLY',
            'WEEKLY',
            'DAILY'
        )),

    CONSTRAINT chk_employment_contracts_dates
        CHECK (
            (end_date IS NULL OR end_date >= start_date)
            AND
            (
                termination_date IS NULL
                OR termination_date >= start_date
            )
        ),

    CONSTRAINT chk_employment_contracts_hours
        CHECK (
            weekly_hours IS NULL
            OR weekly_hours BETWEEN 0 AND 168
        )
);

CREATE INDEX IF NOT EXISTS idx_employment_contracts_employee
    ON employment_contracts(employee_id);

CREATE INDEX IF NOT EXISTS idx_employment_contracts_status
    ON employment_contracts(status);

CREATE INDEX IF NOT EXISTS idx_employment_contracts_end_date
    ON employment_contracts(end_date);


-- ============================================================
-- 2. ADENDAS CONTRACTUALES
-- ============================================================

CREATE TABLE IF NOT EXISTS employment_contract_amendments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    contract_id UUID NOT NULL,

    amendment_number VARCHAR(80) NOT NULL,

    amendment_type VARCHAR(30) NOT NULL,

    effective_date DATE NOT NULL,

    description TEXT NOT NULL,

    previous_values JSONB NULL,
    new_values JSONB NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',

    signed_at TIMESTAMPTZ NULL,

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_contract_amendment_number
        UNIQUE (contract_id, amendment_number),

    CONSTRAINT fk_contract_amendments_contract
        FOREIGN KEY (contract_id)
        REFERENCES employment_contracts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_contract_amendments_user
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_contract_amendments_type
        CHECK (amendment_type IN (
            'SALARY_CHANGE',
            'SCHEDULE_CHANGE',
            'TERM_EXTENSION',
            'POSITION_CHANGE',
            'OTHER'
        )),

    CONSTRAINT chk_contract_amendments_status
        CHECK (status IN (
            'DRAFT',
            'PENDING_SIGNATURE',
            'SIGNED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_contract_amendments_contract
    ON employment_contract_amendments(contract_id);


-- ============================================================
-- 3. HISTORIAL CONTRACTUAL
-- ============================================================

CREATE TABLE IF NOT EXISTS employment_contract_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    contract_id UUID NOT NULL,

    action VARCHAR(40) NOT NULL,

    previous_status VARCHAR(30) NULL,
    new_status VARCHAR(30) NULL,

    details JSONB NULL,

    reason TEXT NULL,

    performed_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_contract_history_contract
        FOREIGN KEY (contract_id)
        REFERENCES employment_contracts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_contract_history_user
        FOREIGN KEY (performed_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_contract_history_action
        CHECK (action IN (
            'CREATED',
            'UPDATED',
            'SIGNED',
            'ACTIVATED',
            'AMENDED',
            'RENEWED',
            'EXPIRED',
            'TERMINATED',
            'CANCELLED'
        ))
);

CREATE INDEX IF NOT EXISTS idx_contract_history_contract
    ON employment_contract_history(contract_id, created_at DESC);


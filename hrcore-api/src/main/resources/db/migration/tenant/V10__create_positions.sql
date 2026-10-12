-- ============================================================
-- V10__create_positions.sql
-- HRCore - Gestión de cargos y requisitos laborales
-- ============================================================
-- Incluye:
-- 1. Catálogo de cargos
-- 2. Niveles organizacionales
-- 3. Responsabilidades
-- 4. Rangos salariales referenciales
-- 5. Requisitos académicos y profesionales
-- ============================================================


-- ============================================================
-- 1. CARGOS
-- ============================================================

CREATE TABLE IF NOT EXISTS positions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,

    description TEXT,
    responsibilities TEXT,

    organizational_level VARCHAR(30) NOT NULL DEFAULT 'OPERATIONAL',

    salary_min NUMERIC(19,4) NULL,
    salary_max NUMERIC(19,4) NULL,
    currency VARCHAR(3) NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,
    updated_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_positions_code
        UNIQUE (code),

    CONSTRAINT fk_positions_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_positions_updated_by
        FOREIGN KEY (updated_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_positions_level
        CHECK (organizational_level IN (
            'EXECUTIVE',
            'MANAGEMENT',
            'SUPERVISORY',
            'PROFESSIONAL',
            'TECHNICAL',
            'OPERATIONAL'
        )),

    CONSTRAINT chk_positions_status
        CHECK (status IN (
            'ACTIVE',
            'INACTIVE'
        )),

    CONSTRAINT chk_positions_salary
        CHECK (
            (salary_min IS NULL OR salary_min >= 0)
            AND
            (salary_max IS NULL OR salary_max >= 0)
            AND
            (
                salary_min IS NULL
                OR salary_max IS NULL
                OR salary_min <= salary_max
            )
        ),

    CONSTRAINT chk_positions_currency
        CHECK (
            currency IS NULL
            OR currency ~ '^[A-Z]{3}$'
        ),

    CONSTRAINT chk_positions_code_not_blank
        CHECK (BTRIM(code) <> ''),

    CONSTRAINT chk_positions_name_not_blank
        CHECK (BTRIM(name) <> '')
);

CREATE INDEX IF NOT EXISTS idx_positions_status
    ON positions(status);

CREATE INDEX IF NOT EXISTS idx_positions_level
    ON positions(organizational_level);


-- ============================================================
-- 2. REQUISITOS DE CARGOS
-- ============================================================

CREATE TABLE IF NOT EXISTS position_requirements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    position_id UUID NOT NULL,

    requirement_type VARCHAR(30) NOT NULL,

    description TEXT NOT NULL,

    mandatory BOOLEAN NOT NULL DEFAULT TRUE,

    minimum_years_experience INTEGER NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_position_requirements_position
        FOREIGN KEY (position_id)
        REFERENCES positions(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_position_requirements_type
        CHECK (requirement_type IN (
            'EDUCATION',
            'EXPERIENCE',
            'SKILL',
            'CERTIFICATION',
            'OTHER'
        )),

    CONSTRAINT chk_position_requirements_experience
        CHECK (
            minimum_years_experience IS NULL
            OR minimum_years_experience >= 0
        )
);

CREATE INDEX IF NOT EXISTS idx_position_requirements_position
    ON position_requirements(position_id);


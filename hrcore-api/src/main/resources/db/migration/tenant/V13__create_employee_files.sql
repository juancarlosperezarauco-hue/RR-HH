-- ============================================================
-- V13__create_employee_files.sql
-- HRCore - Expedientes y documentos de empleados
-- ============================================================
-- Incluye:
-- 1. Expediente principal
-- 2. Clasificación de documentos
-- 3. Documentos laborales
-- 4. Versionado documental
-- 5. Confidencialidad
-- 6. Verificación y archivado
-- ============================================================


-- ============================================================
-- 1. EXPEDIENTES
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_files (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_id UUID NOT NULL,

    file_number VARCHAR(80) NOT NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    notes TEXT NULL,

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employee_files_employee
        UNIQUE (employee_id),

    CONSTRAINT uq_employee_files_number
        UNIQUE (file_number),

    CONSTRAINT fk_employee_files_employee
        FOREIGN KEY (employee_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_files_user
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_files_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        ))
);


-- ============================================================
-- 2. DOCUMENTOS DE EMPLEADOS
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    employee_file_id UUID NOT NULL,

    contract_id UUID NULL,

    document_type VARCHAR(40) NOT NULL,

    title VARCHAR(200) NOT NULL,
    description TEXT NULL,

    document_number VARCHAR(100) NULL,

    issued_at DATE NULL,
    expires_at DATE NULL,

    confidential BOOLEAN NOT NULL DEFAULT FALSE,

    verification_status VARCHAR(20)
        NOT NULL DEFAULT 'PENDING',

    verified_by UUID NULL,
    verified_at TIMESTAMPTZ NULL,

    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',

    created_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_employee_documents_file
        FOREIGN KEY (employee_file_id)
        REFERENCES employee_files(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_documents_contract
        FOREIGN KEY (contract_id)
        REFERENCES employment_contracts(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_documents_verified_by
        FOREIGN KEY (verified_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_employee_documents_created_by
        FOREIGN KEY (created_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_documents_type
        CHECK (document_type IN (
            'IDENTITY',
            'CONTRACT',
            'CERTIFICATE',
            'EDUCATION',
            'MEDICAL',
            'DISCIPLINARY',
            'PAYROLL',
            'OTHER'
        )),

    CONSTRAINT chk_employee_documents_verification
        CHECK (verification_status IN (
            'PENDING',
            'VERIFIED',
            'REJECTED'
        )),

    CONSTRAINT chk_employee_documents_status
        CHECK (status IN (
            'ACTIVE',
            'ARCHIVED'
        )),

    CONSTRAINT chk_employee_documents_dates
        CHECK (
            expires_at IS NULL
            OR issued_at IS NULL
            OR expires_at >= issued_at
        )
);

CREATE INDEX IF NOT EXISTS idx_employee_documents_file
    ON employee_documents(employee_file_id);

CREATE INDEX IF NOT EXISTS idx_employee_documents_type
    ON employee_documents(document_type);

CREATE INDEX IF NOT EXISTS idx_employee_documents_expiration
    ON employee_documents(expires_at);


-- ============================================================
-- 3. VERSIONES DOCUMENTALES
-- ============================================================

CREATE TABLE IF NOT EXISTS employee_document_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    document_id UUID NOT NULL,

    version_number INTEGER NOT NULL,

    original_filename VARCHAR(255) NOT NULL,

    storage_key VARCHAR(500) NOT NULL,

    mime_type VARCHAR(150) NOT NULL,

    file_size BIGINT NOT NULL,

    checksum_sha256 VARCHAR(64) NULL,

    uploaded_by UUID NULL,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_employee_document_version
        UNIQUE (document_id, version_number),

    CONSTRAINT fk_employee_document_versions_document
        FOREIGN KEY (document_id)
        REFERENCES employee_documents(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_employee_document_versions_user
        FOREIGN KEY (uploaded_by)
        REFERENCES tenant_users(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_employee_document_version_positive
        CHECK (version_number > 0),

    CONSTRAINT chk_employee_document_size
        CHECK (file_size >= 0)
);

CREATE INDEX IF NOT EXISTS idx_employee_document_versions_document
    ON employee_document_versions(document_id, version_number DESC);


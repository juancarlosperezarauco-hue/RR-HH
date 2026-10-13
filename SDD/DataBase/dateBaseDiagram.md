-- =============================================================================
-- BASE DE DATOS: SISTEMA DE INFORMACIÓN INTEGRAL DE RECURSOS HUMANOS (SAAS)
-- MOTOR: PostgreSQL 16+
-- ARQUITECTURA: Multi-Tenant Aislado (Shared Database, Discriminator Column)
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- =============================================================================
-- DOMINIO 1: TENANCY, SEGURIDAD, ROLES Y AUDITORÍA
-- =============================================================================

CREATE TABLE tenant_setting (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT NOT NULL,
    data_type VARCHAR(30) DEFAULT 'STRING',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_setting UNIQUE (tenant_id, setting_key)
);

CREATE TABLE tenant_user (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    username VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED')),
    is_owner BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_user_email UNIQUE (tenant_id, email)
);

CREATE TABLE tenant_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    description TEXT,
    is_system_role BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_tenant_role_code UNIQUE (tenant_id, code)
);

CREATE TABLE tenant_role_permission (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    role_id UUID NOT NULL REFERENCES tenant_role(id) ON DELETE CASCADE,
    permission_code VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_role_permission UNIQUE (role_id, permission_code)
);

CREATE TABLE tenant_user_role (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
    role_id UUID NOT NULL REFERENCES tenant_role(id) ON DELETE RESTRICT,
    assigned_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_user_role UNIQUE (user_id, role_id)
);

CREATE TABLE tenant_audit_event (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID REFERENCES tenant_user(id) ON DELETE SET NULL,
    action VARCHAR(50) NOT NULL,
    resource_type VARCHAR(100) NOT NULL,
    resource_id VARCHAR(100) NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    payload_before JSONB,
    payload_after JSONB,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- DOMINIO 2: MOTOR DE NOTIFICACIONES Y PLANTILLAS
-- =============================================================================

CREATE TABLE notification_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    channel VARCHAR(30) NOT NULL DEFAULT 'IN_APP' CHECK (channel IN ('IN_APP', 'EMAIL', 'PUSH')),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_notification_template UNIQUE (tenant_id, code, channel)
);

CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_preference (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE CASCADE,
    category VARCHAR(50) NOT NULL,
    channel VARCHAR(30) NOT NULL,
    is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_user_notif_pref UNIQUE (user_id, category, channel)
);

CREATE TABLE notification_delivery (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL REFERENCES notification(id) ON DELETE CASCADE,
    channel VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    sent_at TIMESTAMP WITH TIME ZONE,
    error_message TEXT
);

-- =============================================================================
-- DOMINIO 3: ESTRUCTURA ORGANIZACIONAL Y UBICACIONES
-- =============================================================================

CREATE TABLE work_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(150) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(100) NOT NULL,
    latitude NUMERIC(10, 7) NOT NULL,
    longitude NUMERIC(10, 7) NOT NULL,
    radius_meters INTEGER NOT NULL DEFAULT 100,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE department (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(150) NOT NULL,
    description TEXT,
    parent_department_id UUID REFERENCES department(id) ON DELETE RESTRICT,
    manager_id UUID, -- Referencia diferida a employee(id)
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_department_code UNIQUE (tenant_id, code)
);

CREATE TABLE position (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    department_id UUID NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    code VARCHAR(50) NOT NULL,
    title VARCHAR(150) NOT NULL,
    description TEXT,
    level VARCHAR(50),
    reports_to_position_id UUID REFERENCES position(id) ON DELETE RESTRICT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_position_code UNIQUE (tenant_id, code)
);

CREATE TABLE position_requirement (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    position_id UUID NOT NULL REFERENCES position(id) ON DELETE CASCADE,
    requirement_type VARCHAR(50) NOT NULL CHECK (requirement_type IN ('EDUCATION', 'EXPERIENCE', 'SKILL', 'CERTIFICATION')),
    description TEXT NOT NULL,
    is_mandatory BOOLEAN NOT NULL DEFAULT TRUE
);

-- =============================================================================
-- DOMINIO 4: MAESTRO DE EMPLEADOS Y COMPLEMENTOS
-- =============================================================================

CREATE TABLE employee (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID UNIQUE REFERENCES tenant_user(id) ON DELETE SET NULL,
    department_id UUID NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    position_id UUID NOT NULL REFERENCES position(id) ON DELETE RESTRICT,
    work_location_id UUID REFERENCES work_location(id) ON DELETE RESTRICT,
    employee_code VARCHAR(50) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    document_type VARCHAR(20) NOT NULL DEFAULT 'CI' CHECK (document_type IN ('CI', 'PASSPORT', 'FOREIGNER_ID')),
    document_number VARCHAR(50) NOT NULL,
    birth_date DATE NOT NULL,
    gender VARCHAR(10) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    marital_status VARCHAR(20) CHECK (marital_status IN ('SINGLE', 'MARRIED', 'DIVORCED', 'WIDOWED')),
    personal_email VARCHAR(255),
    work_email VARCHAR(255),
    phone VARCHAR(30),
    address TEXT,
    hire_date DATE NOT NULL,
    termination_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'PROBATION', 'SUSPENDED', 'TERMINATED')),
    photo_url TEXT,
    social_security_code VARCHAR(30), -- Matrícula Caja de Salud (CNS)
    pension_fund_code VARCHAR(30),    -- NUA / CUA Gestora Pública
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_code UNIQUE (tenant_id, employee_code),
    CONSTRAINT uq_employee_doc UNIQUE (tenant_id, document_number)
);

-- Resolver referencia foránea diferida del jefe de departamento
ALTER TABLE department 
ADD CONSTRAINT fk_department_manager FOREIGN KEY (manager_id) REFERENCES employee(id) ON DELETE SET NULL;

CREATE TABLE employee_emergency_contact (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    name VARCHAR(150) NOT NULL,
    relationship VARCHAR(50) NOT NULL,
    phone VARCHAR(30) NOT NULL,
    alternative_phone VARCHAR(30),
    is_primary BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE employee_bank_account (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    bank_name VARCHAR(100) NOT NULL,
    account_number VARCHAR(50) NOT NULL,
    account_type VARCHAR(30) NOT NULL CHECK (account_type IN ('SAVINGS', 'CHECKING')),
    is_primary BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE employment_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    department_id UUID NOT NULL REFERENCES department(id) ON DELETE RESTRICT,
    position_id UUID NOT NULL REFERENCES position(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE,
    change_reason VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ARCHIVED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- DOMINIO 5: CONTRATACIÓN Y GESTIÓN CONTRACTUAL
-- =============================================================================

CREATE TABLE employment_contract (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE RESTRICT,
    contract_number VARCHAR(50) NOT NULL,
    contract_type VARCHAR(50) NOT NULL CHECK (contract_type IN ('INDEFINITE', 'FIXED_TERM', 'TEMPORARY', 'PROJECT_BASED', 'INTERNSHIP')),
    start_date DATE NOT NULL,
    end_date DATE,
    base_salary NUMERIC(12, 2) NOT NULL CHECK (base_salary >= 0),
    currency VARCHAR(10) NOT NULL DEFAULT 'BOB',
    pay_frequency VARCHAR(20) NOT NULL DEFAULT 'MONTHLY' CHECK (pay_frequency IN ('MONTHLY', 'BIWEEKLY')),
    working_hours_per_week NUMERIC(4, 2) NOT NULL DEFAULT 48.0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PENDING', 'ACTIVE', 'EXPIRING', 'EXPIRED', 'TERMINATED', 'AMENDED')),
    signed_at DATE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contract_number UNIQUE (tenant_id, contract_number),
    CONSTRAINT chk_contract_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

CREATE TABLE employment_contract_amendment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    contract_id UUID NOT NULL REFERENCES employment_contract(id) ON DELETE CASCADE,
    amendment_number VARCHAR(50) NOT NULL,
    issue_date DATE NOT NULL,
    effective_date DATE NOT NULL,
    change_type VARCHAR(50) NOT NULL CHECK (change_type IN ('SALARY_INCREASE', 'POSITION_CHANGE', 'SCHEDULE_CHANGE', 'TERMS_UPDATE')),
    previous_value TEXT NOT NULL,
    new_value TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- DOMINIO 6: EXPEDIENTE LABORAL DIGITAL (HU15)
-- =============================================================================

CREATE TABLE document_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL,
    category VARCHAR(50) NOT NULL CHECK (category IN ('IDENTIFICATION', 'CONTRACT', 'EDUCATION', 'HEALTH', 'DISCIPLINARY', 'ASSET')),
    is_mandatory BOOLEAN NOT NULL DEFAULT FALSE,
    has_expiration BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uq_document_type_code UNIQUE (tenant_id, code)
);

CREATE TABLE employee_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    document_type_id UUID NOT NULL REFERENCES document_type(id) ON DELETE RESTRICT,
    document_number VARCHAR(100),
    file_name VARCHAR(255) NOT NULL,
    file_path TEXT NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    version_number INTEGER NOT NULL DEFAULT 1,
    issue_date DATE,
    expiration_date DATE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'EXPIRED', 'ARCHIVED')),
    verified_by UUID REFERENCES tenant_user(id) ON DELETE SET NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE employee_document_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL REFERENCES employee_document(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES tenant_user(id) ON DELETE RESTRICT,
    action VARCHAR(50) NOT NULL CHECK (action IN ('UPLOAD', 'VIEW', 'DOWNLOAD', 'UPDATE', 'ARCHIVE')),
    performed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45)
);

-- =============================================================================
-- DOMINIO 7: PARAMETRIZACIÓN Y ASIGNACIÓN DE TURNOS (HU16)
-- =============================================================================

CREATE TABLE shift (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    shift_type VARCHAR(30) NOT NULL CHECK (shift_type IN ('CONTINUOUS', 'SPLIT', 'ROTATING', 'NIGHT')),
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    break_minutes INTEGER NOT NULL DEFAULT 0,
    grace_period_minutes INTEGER NOT NULL DEFAULT 10,
    is_overnight BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_shift_code UNIQUE (tenant_id, code)
);

CREATE TABLE shift_day (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    shift_id UUID NOT NULL REFERENCES shift(id) ON DELETE CASCADE,
    day_of_week INTEGER NOT NULL CHECK (day_of_week BETWEEN 1 AND 7), -- 1: Lunes, 7: Domingo
    is_work_day BOOLEAN NOT NULL DEFAULT TRUE,
    start_time TIME,
    end_time TIME,
    break_minutes INTEGER DEFAULT 0,
    CONSTRAINT uq_shift_day UNIQUE (shift_id, day_of_week)
);

CREATE TABLE employee_shift_assignment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    shift_id UUID NOT NULL REFERENCES shift(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE,
    is_recurring BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_shift_assign_dates CHECK (end_date IS NULL OR end_date >= start_date)
);

-- =============================================================================
-- DOMINIO 8: CONTROL DE ASISTENCIA Y MARCACIONES (HU17)
-- =============================================================================

CREATE TABLE attendance_record (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    shift_id UUID REFERENCES shift(id) ON DELETE SET NULL,
    work_date DATE NOT NULL,
    check_in_time TIMESTAMP WITH TIME ZONE,
    check_out_time TIMESTAMP WITH TIME ZONE,
    worked_hours NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    overtime_hours NUMERIC(5, 2) NOT NULL DEFAULT 0.0,
    late_minutes INTEGER NOT NULL DEFAULT 0,
    early_departure_minutes INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'PRESENT' CHECK (status IN ('PRESENT', 'LATE', 'EARLY_DEPARTURE', 'ABSENT', 'EXCUSED', 'HOLIDAY', 'REST_DAY')),
    source VARCHAR(20) NOT NULL DEFAULT 'WEB' CHECK (source IN ('WEB', 'MOBILE', 'BIOMETRIC', 'MANUAL')),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_work_date UNIQUE (tenant_id, employee_id, work_date)
);

CREATE TABLE attendance_punch (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    attendance_record_id UUID NOT NULL REFERENCES attendance_record(id) ON DELETE CASCADE,
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    punch_time TIMESTAMP WITH TIME ZONE NOT NULL,
    punch_type VARCHAR(20) NOT NULL CHECK (punch_type IN ('IN', 'OUT', 'BREAK_OUT', 'BREAK_IN')),
    source VARCHAR(20) NOT NULL DEFAULT 'WEB' CHECK (source IN ('WEB', 'MOBILE', 'BIOMETRIC')),
    device_id VARCHAR(100),
    latitude NUMERIC(10, 7),
    longitude NUMERIC(10, 7),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE attendance_correction (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    attendance_record_id UUID NOT NULL REFERENCES attendance_record(id) ON DELETE CASCADE,
    requested_by UUID NOT NULL REFERENCES tenant_user(id) ON DELETE RESTRICT,
    approved_by UUID REFERENCES tenant_user(id) ON DELETE RESTRICT,
    original_check_in TIMESTAMP WITH TIME ZONE,
    corrected_check_in TIMESTAMP WITH TIME ZONE,
    original_check_out TIMESTAMP WITH TIME ZONE,
    corrected_check_out TIMESTAMP WITH TIME ZONE,
    reason TEXT NOT NULL,
    decision_notes TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    decided_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE attendance_overtime (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    attendance_record_id UUID NOT NULL REFERENCES attendance_record(id) ON DELETE CASCADE,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    overtime_hours NUMERIC(4, 2) NOT NULL,
    overtime_type VARCHAR(30) NOT NULL CHECK (overtime_type IN ('DAYTIME', 'NIGHT', 'HOLIDAY', 'REST_DAY')),
    rate_multiplier NUMERIC(3, 2) NOT NULL DEFAULT 2.0, -- Recargo LGT Bolivia: 2.0x
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approved_by UUID REFERENCES tenant_user(id) ON DELETE RESTRICT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- =============================================================================
-- DOMINIO 9: FERIADOS, AUSENCIAS Y VACACIONES (HU18)
-- =============================================================================

CREATE TABLE holiday (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    holiday_date DATE NOT NULL,
    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
    scope VARCHAR(20) NOT NULL DEFAULT 'NATIONAL' CHECK (scope IN ('NATIONAL', 'REGIONAL')),
    CONSTRAINT uq_tenant_holiday UNIQUE (tenant_id, holiday_date)
);

CREATE TABLE leave_type (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(100) NOT NULL,
    is_paid BOOLEAN NOT NULL DEFAULT TRUE,
    requires_approval BOOLEAN NOT NULL DEFAULT TRUE,
    requires_attachment BOOLEAN NOT NULL DEFAULT FALSE,
    max_days_per_year NUMERIC(4, 1),
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'INACTIVE')),
    CONSTRAINT uq_leave_type_code UNIQUE (tenant_id, code)
);

CREATE TABLE leave_balance (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    year INTEGER NOT NULL,
    entitled_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    used_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    pending_days NUMERIC(4, 1) NOT NULL DEFAULT 0.0,
    remaining_days NUMERIC(4, 1) GENERATED ALWAYS AS (entitled_days - used_days - pending_days) STORED,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_employee_leave_balance UNIQUE (tenant_id, employee_id, leave_type_id, year)
);

CREATE TABLE leave_request (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    employee_id UUID NOT NULL REFERENCES employee(id) ON DELETE CASCADE,
    leave_type_id UUID NOT NULL REFERENCES leave_type(id) ON DELETE RESTRICT,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    total_days NUMERIC(4, 1) NOT NULL,
    is_hourly BOOLEAN NOT NULL DEFAULT FALSE,
    start_time TIME,
    end_time TIME,
    total_hours NUMERIC(4, 2),
    reason TEXT NOT NULL,
    attachment_url TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED')),
    reviewed_by UUID REFERENCES tenant_user(id) ON DELETE RESTRICT,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    rejection_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_leave_dates CHECK (end_date >= start_date)
);

-- =============================================================================
-- ÍNDICES ESTRATÉGICOS DE RENDIMIENTO Y PARTICIÓN MULTI-TENANT
-- =============================================================================

CREATE INDEX idx_user_tenant ON tenant_user(tenant_id);
CREATE INDEX idx_employee_tenant ON employee(tenant_id);
CREATE INDEX idx_employee_dept ON employee(department_id);
CREATE INDEX idx_employee_position ON employee(position_id);
CREATE INDEX idx_contract_employee ON employment_contract(employee_id);
CREATE INDEX idx_contract_tenant ON employment_contract(tenant_id);
CREATE INDEX idx_document_employee ON employee_document(employee_id);
CREATE INDEX idx_document_tenant ON employee_document(tenant_id);
CREATE INDEX idx_shift_assignment_emp ON employee_shift_assignment(employee_id);
CREATE INDEX idx_shift_assignment_tenant ON employee_shift_assignment(tenant_id);
CREATE INDEX idx_attendance_emp_date ON attendance_record(tenant_id, employee_id, work_date);
CREATE INDEX idx_punch_record ON attendance_punch(attendance_record_id);
CREATE INDEX idx_punch_emp_time ON attendance_punch(tenant_id, employee_id, punch_time);
CREATE INDEX idx_leave_emp_status ON leave_request(tenant_id, employee_id, status);
CREATE INDEX idx_audit_tenant_res ON tenant_audit_event(tenant_id, resource_type, resource_id);
-- Gestión contractual completa sin modificar la migración V12 ya aplicada.

ALTER TABLE employment_contracts
    ADD COLUMN IF NOT EXISTS employee_assignment_id UUID NULL,
    ADD COLUMN IF NOT EXISTS contract_form VARCHAR(20) NOT NULL DEFAULT 'WRITTEN',
    ADD COLUMN IF NOT EXISTS work_schedule VARCHAR(20) NOT NULL DEFAULT 'FULL_TIME',
    ADD COLUMN IF NOT EXISTS work_modality VARCHAR(20) NOT NULL DEFAULT 'ONSITE',
    ADD COLUMN IF NOT EXISTS renewed_from_contract_id UUID NULL;

ALTER TABLE employment_contracts
    DROP CONSTRAINT IF EXISTS chk_employment_contracts_type;

ALTER TABLE employment_contracts
    ADD CONSTRAINT chk_employment_contracts_type
        CHECK (contract_type IN (
            'INDEFINITE', 'FIXED_TERM', 'SEASONAL', 'WORK_OR_SERVICE',
            'EVENTUAL', 'INTERNSHIP', 'OTHER',
            -- Valores heredados de V12 para conservar compatibilidad.
            'PART_TIME', 'TEMPORARY'
        ));

ALTER TABLE employment_contracts
    DROP CONSTRAINT IF EXISTS chk_employment_contracts_form,
    DROP CONSTRAINT IF EXISTS chk_employment_contracts_schedule,
    DROP CONSTRAINT IF EXISTS chk_employment_contracts_modality;

ALTER TABLE employment_contracts
    ADD CONSTRAINT chk_employment_contracts_form
        CHECK (contract_form IN ('WRITTEN', 'VERBAL')),
    ADD CONSTRAINT chk_employment_contracts_schedule
        CHECK (work_schedule IN ('FULL_TIME', 'PART_TIME')),
    ADD CONSTRAINT chk_employment_contracts_modality
        CHECK (work_modality IN ('ONSITE', 'REMOTE', 'HYBRID'));

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_employment_contracts_assignment'
          AND conrelid = 'employment_contracts'::regclass
    ) THEN
        ALTER TABLE employment_contracts
            ADD CONSTRAINT fk_employment_contracts_assignment
            FOREIGN KEY (employee_assignment_id)
            REFERENCES employee_assignments(id)
            ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_employment_contracts_renewed_from'
          AND conrelid = 'employment_contracts'::regclass
    ) THEN
        ALTER TABLE employment_contracts
            ADD CONSTRAINT fk_employment_contracts_renewed_from
            FOREIGN KEY (renewed_from_contract_id)
            REFERENCES employment_contracts(id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_employment_contracts_assignment
    ON employment_contracts(employee_assignment_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_employment_contracts_renewed_from
    ON employment_contracts(renewed_from_contract_id)
    WHERE renewed_from_contract_id IS NOT NULL;

-- Permisos del módulo para el administrador propietario de tenants existentes.
INSERT INTO tenant_role_permissions (role_id, permission_code, assigned_at)
SELECT r.id, p.code, NOW()
FROM tenant_roles r
CROSS JOIN (VALUES
    ('contracts.create'),
    ('contracts.list'),
    ('contracts.detail'),
    ('contracts.update'),
    ('contracts.status.update'),
    ('contracts.renew'),
    ('contracts.history.read'),
    ('employees.list'),
    ('employees.detail'),
    ('employees.assignments.list'),
    ('employees.assignments.detail'),
    ('departments.list'),
    ('departments.detail'),
    ('positions.list'),
    ('positions.detail')
) AS p(code)
JOIN public.system_permissions sp ON sp.code = p.code AND sp.active = TRUE
WHERE r.code = 'OWNER_ADMIN'
ON CONFLICT (role_id, permission_code) DO NOTHING;



ALTER TABLE employment_contracts
    ADD COLUMN IF NOT EXISTS employee_assignment_id UUID NOT NULL,
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
            FOREIGN KEY (employee_assignment_id,employee_id)
            REFERENCES employee_assignments(id,employee_id)
            ON DELETE RESTRICT;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_employment_contracts_renewed_from'
          AND conrelid = 'employment_contracts'::regclass
    ) THEN
        ALTER TABLE employment_contracts
            ADD CONSTRAINT fk_employment_contracts_renewed_from
            FOREIGN KEY (renewed_from_contract_id,employee_id)
            REFERENCES employment_contracts(id,employee_id)
            ON DELETE RESTRICT;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_employment_contracts_assignment
    ON employment_contracts(employee_assignment_id);

ALTER TABLE employment_contracts
    ADD CONSTRAINT chk_contract_no_self_renewal
    CHECK (renewed_from_contract_id IS NULL OR renewed_from_contract_id <> id);

-- Comprobar pertenencia al mismo periodo; la FK compuesta también comprueba
-- que la asignación y el contrato pertenecen al mismo empleado.
CREATE FUNCTION validate_contract_lineage()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE assignment_period UUID; assignment_kind VARCHAR(30);
        assignment_status_value VARCHAR(20); assignment_start DATE; assignment_end DATE;
        parent_period UUID; cycle_found BOOLEAN;
BEGIN
    IF NEW.employee_assignment_id IS NOT NULL THEN
        SELECT employment_period_id, assignment_type, assignment_status, start_date, end_date
          INTO assignment_period, assignment_kind, assignment_status_value, assignment_start, assignment_end
          FROM employee_assignments WHERE id=NEW.employee_assignment_id AND employee_id=NEW.employee_id;
        IF assignment_period IS NULL OR assignment_period <> NEW.employment_period_id THEN
            RAISE EXCEPTION 'Contract assignment must belong to the same employment period' USING ERRCODE='23514';
        END IF;
        IF assignment_kind <> 'PRIMARY' OR assignment_status_value <> 'ACTIVE'
           OR assignment_start > NEW.start_date
           OR (assignment_end IS NOT NULL AND assignment_end < NEW.start_date) THEN
            RAISE EXCEPTION 'Contract requires an active PRIMARY assignment at its start date' USING ERRCODE='23514';
        END IF;
    END IF;
    IF NEW.renewed_from_contract_id IS NOT NULL THEN
        SELECT employment_period_id INTO parent_period
          FROM employment_contracts WHERE id=NEW.renewed_from_contract_id AND employee_id=NEW.employee_id;
        IF parent_period IS NULL OR parent_period <> NEW.employment_period_id THEN
            RAISE EXCEPTION 'Renewal source must belong to the same employment period' USING ERRCODE='23514';
        END IF;
        WITH RECURSIVE chain(id, parent_id, visited) AS (
            SELECT c.id,c.renewed_from_contract_id,ARRAY[c.id]
              FROM employment_contracts c WHERE c.id=NEW.renewed_from_contract_id
            UNION ALL
            SELECT c.id,c.renewed_from_contract_id,chain.visited||c.id
              FROM employment_contracts c JOIN chain ON c.id=chain.parent_id
             WHERE NOT (c.id=ANY(chain.visited))
        )
        SELECT EXISTS(SELECT 1 FROM chain WHERE id=NEW.id) INTO cycle_found;
        IF cycle_found OR NEW.renewed_from_contract_id=NEW.id THEN
            RAISE EXCEPTION 'Contract renewal cannot form a cycle' USING ERRCODE='23514';
        END IF;
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_validate_contract_lineage
BEFORE INSERT OR UPDATE OF employee_id, employee_assignment_id, employment_period_id, renewed_from_contract_id
ON employment_contracts FOR EACH ROW EXECUTE FUNCTION validate_contract_lineage();

-- Una asignación utilizada por un contrato no debe modificarse de modo que
-- deje de ser su referencia organizacional válida al inicio del contrato.
CREATE FUNCTION protect_contract_origin_assignment()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM employment_contracts c
         WHERE c.employee_assignment_id=NEW.id AND c.status <> 'CANCELLED'
           AND (NEW.assignment_type <> 'PRIMARY'
                OR NEW.assignment_status <> 'ACTIVE'
                OR NEW.start_date > c.start_date
                OR (NEW.end_date IS NOT NULL AND NEW.end_date < c.start_date)
                OR NEW.employment_period_id <> c.employment_period_id)
    ) THEN
        RAISE EXCEPTION 'Assignment change would invalidate contract origin' USING ERRCODE='23514';
    END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_protect_contract_origin_assignment
BEFORE UPDATE OF start_date,end_date,assignment_type,assignment_status,employment_period_id
ON employee_assignments FOR EACH ROW EXECUTE FUNCTION protect_contract_origin_assignment();


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

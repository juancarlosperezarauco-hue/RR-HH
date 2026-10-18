-- ============================================================
-- 1. SECUENCIA DE CÓDIGOS DE ROLES
-- ============================================================

-- Cada tenant posee su propia secuencia.
-- Formato: ROL-00001, ROL-00002, ...
-- Los números no se reutilizan.

CREATE SEQUENCE tenant_role_code_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1
    NO CYCLE;


-- ============================================================
-- 2. ROLES DEL TENANT
-- ============================================================

CREATE TABLE tenant_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL,

    name VARCHAR(100) NOT NULL,

    description VARCHAR(255),

    role_type VARCHAR(20) NOT NULL DEFAULT 'INTERNAL',

    system_role BOOLEAN NOT NULL DEFAULT FALSE,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenant_roles_code UNIQUE (code),

    CONSTRAINT ck_tenant_roles_code CHECK (
        code = 'OWNER_ADMIN'
        OR code ~ '^ROL-[0-9]{5,}$'
    ),

    CONSTRAINT ck_tenant_roles_role_type CHECK (
        (
            code = 'OWNER_ADMIN'
            AND role_type = 'ROOT'
            AND system_role = TRUE
        )
        OR
        (
            code ~ '^ROL-[0-9]{5,}$'
            AND role_type = 'INTERNAL'
            AND system_role = FALSE
        )
    ),

    CONSTRAINT ck_tenant_roles_owner_active CHECK (
        code <> 'OWNER_ADMIN'
        OR active = TRUE
    ),
    CONSTRAINT ck_tenant_roles_reserved_custom_codes CHECK (
        code = 'OWNER_ADMIN' OR (code <> 'SUPERADMIN' AND LEFT(code, 5) <> 'ROLE_')
    )
);

CREATE UNIQUE INDEX uq_tenant_roles_name_ci
    ON tenant_roles (LOWER(name));

CREATE INDEX idx_tenant_roles_active
    ON tenant_roles (active);

CREATE INDEX idx_tenant_roles_role_type
    ON tenant_roles (role_type);


-- ============================================================
-- 3. GENERACIÓN AUTOMÁTICA DE CÓDIGOS
-- ============================================================

-- ROOT:
--   Identificador reservado OWNER_ADMIN.
--   Solamente se crea durante la inicialización del tenant.
--
-- INTERNAL:
--   El código se genera siempre en PostgreSQL.
--   Cualquier código recibido para un rol interno se ignora.
--
-- La secuencia se obtiene del esquema que activó el trigger.
-- Esto evita utilizar accidentalmente la secuencia
-- correspondiente a otro tenant.

CREATE FUNCTION generate_tenant_role_code()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_sequence_name REGCLASS;
    v_sequence_number BIGINT;
BEGIN

    -- Rol reservado del propietario.
    IF NEW.code = 'OWNER_ADMIN' THEN

        IF NEW.role_type IS DISTINCT FROM 'ROOT'
           OR NEW.system_role IS DISTINCT FROM TRUE THEN

            RAISE EXCEPTION
                'OWNER_ADMIN requires ROOT role type'
                USING ERRCODE = '23514';

        END IF;

        RETURN NEW;
    END IF;

    -- Solamente se permiten roles internos personalizados.
    IF NEW.role_type IS DISTINCT FROM 'INTERNAL'
       OR NEW.system_role IS DISTINCT FROM FALSE THEN

        RAISE EXCEPTION
            'Custom roles must be INTERNAL'
            USING ERRCODE = '23514';

    END IF;

    -- Resolver la secuencia del tenant actual.
    v_sequence_name := FORMAT(
        '%I.%I',
        TG_TABLE_SCHEMA,
        'tenant_role_code_seq'
    )::REGCLASS;

    -- Obtener siguiente número de forma segura.
    v_sequence_number := NEXTVAL(v_sequence_name);

    -- Generar código inmutable.
    NEW.code :=
        'ROL-' ||
        LPAD(v_sequence_number::TEXT, 5, '0');

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_generate_tenant_role_code
BEFORE INSERT ON tenant_roles
FOR EACH ROW
EXECUTE FUNCTION generate_tenant_role_code();


-- ============================================================
-- 4. ASIGNACIÓN DE ROLES A USUARIOS
-- ============================================================

CREATE TABLE tenant_user_roles (
    user_id UUID NOT NULL,

    role_id UUID NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (user_id, role_id),

    CONSTRAINT fk_tenant_user_roles_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tenant_user_roles_role
        FOREIGN KEY (role_id)
        REFERENCES tenant_roles(id)
        ON DELETE RESTRICT
);

CREATE INDEX idx_tenant_user_roles_role
    ON tenant_user_roles (role_id);


-- ============================================================
-- 5. PERMISOS ASIGNADOS A ROLES
-- ============================================================

CREATE TABLE tenant_role_permissions (
    role_id UUID NOT NULL,

    permission_code VARCHAR(100) NOT NULL,

    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    PRIMARY KEY (role_id, permission_code),

    CONSTRAINT fk_tenant_role_permissions_role
        FOREIGN KEY (role_id)
        REFERENCES tenant_roles(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_tenant_role_permissions_catalog
        FOREIGN KEY (permission_code)
        REFERENCES public.system_permissions(code)
        ON DELETE RESTRICT
);

CREATE INDEX idx_tenant_role_permissions_code
    ON tenant_role_permissions (permission_code);


-- ============================================================
-- 6. ESTADO DE AUTORIZACIÓN DEL TENANT
-- ============================================================

-- owner_user_id identifica al propietario real.
-- No se deduce exclusivamente del rol ROOT.

CREATE TABLE tenant_authorization_state (
    id SMALLINT PRIMARY KEY,

    owner_user_id UUID NULL,

    authorization_version BIGINT NOT NULL DEFAULT 1,

    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_tenant_authorization_state_singleton
        CHECK (id = 1),

    CONSTRAINT ck_tenant_authorization_version
        CHECK (authorization_version > 0),

    CONSTRAINT fk_tenant_authorization_owner
        FOREIGN KEY (owner_user_id)
        REFERENCES tenant_users(id)
        ON DELETE RESTRICT
);

INSERT INTO tenant_authorization_state (
    id,
    owner_user_id,
    authorization_version,
    updated_at
)
VALUES (
    1,
    NULL,
    1,
    NOW()
);


-- ============================================================
-- 7. CREACIÓN DEL ROL ROOT
-- ============================================================

-- Existe exactamente un OWNER_ADMIN por tenant.
-- La restricción UNIQUE impide duplicar el código.
-- Su usuario propietario se registra durante el onboarding.

INSERT INTO tenant_roles (
    code,
    name,
    description,
    role_type,
    system_role,
    active,
    created_at,
    updated_at
)
VALUES (
    'OWNER_ADMIN',
    'OWNER_ADMIN',
    'Tenant owner administrator role',
    'ROOT',
    TRUE,
    TRUE,
    NOW(),
    NOW()
);


-- ============================================================
-- 8. PROTECCIÓN DE ROLES
-- ============================================================

-- ROOT:
--   No se modifica ni elimina.
--
-- INTERNAL:
--   Puede modificar nombre, descripción y estado.
--   No puede cambiar código, tipo ni system_role.

CREATE FUNCTION protect_system_role()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN

    IF TG_OP = 'DELETE' THEN

        IF OLD.role_type = 'ROOT'
           OR OLD.system_role
           OR OLD.code = 'OWNER_ADMIN' THEN

            RAISE EXCEPTION
                'System role OWNER_ADMIN cannot be deleted'
                USING ERRCODE = '23514';

        END IF;

        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN

        IF OLD.role_type = 'ROOT'
           OR OLD.system_role
           OR OLD.code = 'OWNER_ADMIN' THEN

            RAISE EXCEPTION
                'System role OWNER_ADMIN cannot be modified'
                USING ERRCODE = '23514';

        END IF;

        IF NEW.role_type IS DISTINCT FROM OLD.role_type THEN

            RAISE EXCEPTION
                'Role type is immutable'
                USING ERRCODE = '23514';

        END IF;

        IF NEW.system_role IS DISTINCT FROM OLD.system_role THEN

            RAISE EXCEPTION
                'System role flag is immutable'
                USING ERRCODE = '23514';

        END IF;

        IF NEW.code IS DISTINCT FROM OLD.code THEN

            RAISE EXCEPTION
                'Role code is immutable'
                USING ERRCODE = '23514';

        END IF;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_system_role
BEFORE UPDATE OR DELETE ON tenant_roles
FOR EACH ROW
EXECUTE FUNCTION protect_system_role();


-- ============================================================
-- 9. PROTEGER PERMISOS DEL PROPIETARIO
-- ============================================================

CREATE FUNCTION protect_owner_role_permissions()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_role_id UUID;
BEGIN

    EXECUTE FORMAT(
        'SELECT id FROM %I.tenant_roles
         WHERE code = ''OWNER_ADMIN''',
        TG_TABLE_SCHEMA
    )
    INTO v_owner_role_id;

    IF TG_OP = 'DELETE' THEN

        IF OLD.role_id = v_owner_role_id THEN

            RAISE EXCEPTION
                'OWNER_ADMIN permissions cannot be removed manually'
                USING ERRCODE = '23514';

        END IF;

        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN

        IF OLD.role_id = v_owner_role_id
           OR NEW.role_id = v_owner_role_id THEN

            RAISE EXCEPTION
                'OWNER_ADMIN permissions cannot be modified manually'
                USING ERRCODE = '23514';

        END IF;

        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_owner_role_permissions
BEFORE UPDATE OR DELETE ON tenant_role_permissions
FOR EACH ROW
EXECUTE FUNCTION protect_owner_role_permissions();


-- ============================================================
-- 10. PROTEGER IDENTIDAD DEL PROPIETARIO
-- ============================================================

CREATE FUNCTION protect_owner_state()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_exists BOOLEAN;
BEGIN

    IF OLD.owner_user_id IS NOT NULL
       AND NEW.owner_user_id IS DISTINCT FROM OLD.owner_user_id THEN

        RAISE EXCEPTION
            'Tenant ownership cannot be modified by a generic operation'
            USING ERRCODE = '23514';

    END IF;

    IF OLD.owner_user_id IS NULL
       AND NEW.owner_user_id IS NOT NULL THEN

        EXECUTE FORMAT(
            'SELECT EXISTS (
                SELECT 1
                FROM %I.tenant_users
                WHERE id = $1
            )',
            TG_TABLE_SCHEMA
        )
        INTO v_owner_exists
        USING NEW.owner_user_id;

        IF v_owner_exists IS NOT TRUE THEN

            RAISE EXCEPTION
                'Owner user does not exist'
                USING ERRCODE = '23503';

        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_protect_owner_state
BEFORE UPDATE OF owner_user_id
ON tenant_authorization_state
FOR EACH ROW
EXECUTE FUNCTION protect_owner_state();


-- ============================================================
-- 11. PROTEGER ASIGNACIÓN DEL ROL ROOT
-- ============================================================

CREATE FUNCTION protect_owner_user_role()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_role_id UUID;
    v_owner_user_id UUID;
BEGIN

    EXECUTE FORMAT(
        'SELECT id FROM %I.tenant_roles
         WHERE code = ''OWNER_ADMIN''',
        TG_TABLE_SCHEMA
    )
    INTO v_owner_role_id;

    IF TG_OP = 'INSERT' THEN

        IF NEW.role_id = v_owner_role_id THEN

            EXECUTE FORMAT(
                'SELECT owner_user_id
                 FROM %I.tenant_authorization_state
                 WHERE id = 1
                 FOR UPDATE',
                TG_TABLE_SCHEMA
            )
            INTO v_owner_user_id;

            IF v_owner_user_id IS NULL
               OR v_owner_user_id <> NEW.user_id THEN

                RAISE EXCEPTION
                    'OWNER_ADMIN can only be assigned to the registered owner'
                    USING ERRCODE = '23514';

            END IF;
        END IF;

        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' THEN

        IF OLD.role_id = v_owner_role_id
           OR NEW.role_id = v_owner_role_id THEN

            RAISE EXCEPTION
                'OWNER_ADMIN assignments cannot be modified'
                USING ERRCODE = '23514';

        END IF;

        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN

        IF OLD.role_id = v_owner_role_id THEN

            RAISE EXCEPTION
                'OWNER_ADMIN assignment cannot be removed'
                USING ERRCODE = '23514';

        END IF;

        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_owner_user_role
BEFORE INSERT OR UPDATE OR DELETE
ON tenant_user_roles
FOR EACH ROW
EXECUTE FUNCTION protect_owner_user_role();


-- ============================================================
-- 12. VERIFICAR CONSISTENCIA DEL PROPIETARIO
-- ============================================================

-- Cuando se registra al propietario, debe contar
-- con la asignación OWNER_ADMIN.
--
-- La comprobación es diferida para permitir que
-- el onboarding complete ambas operaciones
-- dentro de una misma transacción.

CREATE FUNCTION assert_owner_assignment_consistency()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_user_id UUID;
    v_has_assignment BOOLEAN;
BEGIN

    EXECUTE FORMAT(
        'SELECT owner_user_id
         FROM %I.tenant_authorization_state
         WHERE id = 1',
        TG_TABLE_SCHEMA
    )
    INTO v_owner_user_id;

    IF v_owner_user_id IS NULL THEN
        RETURN NULL;
    END IF;

    EXECUTE FORMAT(
        'SELECT EXISTS (
            SELECT 1
            FROM %I.tenant_user_roles tur
            JOIN %I.tenant_roles r
                ON r.id = tur.role_id
            WHERE r.code = ''OWNER_ADMIN''
              AND tur.user_id = $1
        )',
        TG_TABLE_SCHEMA,
        TG_TABLE_SCHEMA
    )
    INTO v_has_assignment
    USING v_owner_user_id;

    IF v_has_assignment IS NOT TRUE THEN

        RAISE EXCEPTION
            'Tenant owner has no OWNER_ADMIN assignment'
            USING ERRCODE = '23514';

    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_assert_owner_assignment
AFTER INSERT OR UPDATE
ON tenant_authorization_state
DEFERRABLE INITIALLY DEFERRED
FOR EACH ROW
EXECUTE FUNCTION assert_owner_assignment_consistency();



CREATE OR REPLACE FUNCTION protect_owner_account()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_user_id UUID;
BEGIN
    EXECUTE format(
        'SELECT owner_user_id
         FROM %I.tenant_authorization_state
         WHERE id = 1',
        TG_TABLE_SCHEMA
    )
    INTO v_owner_user_id;

    IF v_owner_user_id = OLD.id THEN

        IF NEW.active = FALSE THEN
            RAISE EXCEPTION
                'Current tenant owner cannot be deactivated'
                USING ERRCODE = '23514';
        END IF;

        IF NEW.status <> 'ACTIVE' THEN
            RAISE EXCEPTION
                'Current tenant owner must remain ACTIVE'
                USING ERRCODE = '23514';
        END IF;

    END IF;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_protect_owner_account
ON tenant_users;

CREATE TRIGGER trg_protect_owner_account
BEFORE UPDATE OF active, status
ON tenant_users
FOR EACH ROW
EXECUTE FUNCTION protect_owner_account();


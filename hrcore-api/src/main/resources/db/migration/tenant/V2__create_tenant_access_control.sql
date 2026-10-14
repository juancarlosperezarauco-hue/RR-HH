
-- ARCHIVO: src/main/resources/db/migration/tenant/V2__create_tenant_access_control.sql

CREATE TABLE tenant_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    system_role BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_tenant_roles_code UNIQUE (code),
    CONSTRAINT ck_tenant_roles_code CHECK (code ~ '^[A-Z][A-Z0-9_]{0,99}$'),
    CONSTRAINT ck_tenant_roles_system_role CHECK (
        (code = 'OWNER_ADMIN' AND system_role = TRUE)
        OR (code <> 'OWNER_ADMIN' AND system_role = FALSE)
    ),
    CONSTRAINT ck_tenant_roles_owner_active CHECK (code <> 'OWNER_ADMIN' OR active = TRUE)
);

CREATE UNIQUE INDEX uq_tenant_roles_name_ci ON tenant_roles (LOWER(name));
CREATE INDEX idx_tenant_roles_active ON tenant_roles (active);

CREATE TABLE tenant_user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_tenant_user_roles_user FOREIGN KEY (user_id) REFERENCES tenant_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_user_roles_role FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE RESTRICT
);

CREATE INDEX idx_tenant_user_roles_role ON tenant_user_roles (role_id);

CREATE TABLE tenant_role_permissions (
    role_id UUID NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_code),
    CONSTRAINT fk_tenant_role_permissions_role FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE RESTRICT,
    CONSTRAINT fk_tenant_role_permissions_catalog FOREIGN KEY (permission_code)
        REFERENCES public.system_permissions(code) ON DELETE RESTRICT
);

CREATE INDEX idx_tenant_role_permissions_code ON tenant_role_permissions (permission_code);

CREATE TABLE tenant_authorization_state (
    id SMALLINT PRIMARY KEY,
    owner_user_id UUID NULL,
    authorization_version BIGINT NOT NULL DEFAULT 1,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tenant_authorization_state_singleton CHECK (id = 1),
    CONSTRAINT ck_tenant_authorization_version CHECK (authorization_version > 0),
    CONSTRAINT fk_tenant_authorization_owner FOREIGN KEY (owner_user_id)
        REFERENCES tenant_users(id) ON DELETE RESTRICT
);

INSERT INTO tenant_authorization_state (id, owner_user_id, authorization_version, updated_at)
VALUES (1, NULL, 1, NOW());

INSERT INTO tenant_roles (code, name, description, system_role, active, created_at, updated_at)
VALUES ('OWNER_ADMIN', 'OWNER_ADMIN', 'Tenant owner administrator role', TRUE, TRUE, NOW(), NOW());

CREATE FUNCTION protect_system_role()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.system_role OR OLD.code = 'OWNER_ADMIN' THEN
            RAISE EXCEPTION 'System role OWNER_ADMIN cannot be deleted' USING ERRCODE = '23514';
        END IF;
        RETURN OLD;
    END IF;
    IF TG_OP = 'UPDATE' THEN
        IF OLD.system_role OR OLD.code = 'OWNER_ADMIN' THEN
            RAISE EXCEPTION 'System role OWNER_ADMIN cannot be modified' USING ERRCODE = '23514';
        END IF;
        IF NEW.system_role OR NEW.code = 'OWNER_ADMIN' THEN
            RAISE EXCEPTION 'Reserved system role cannot be assigned to a custom role' USING ERRCODE = '23514';
        END IF;
        IF NEW.code IS DISTINCT FROM OLD.code THEN
            RAISE EXCEPTION 'Role code is immutable' USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;
    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_system_role
BEFORE UPDATE OR DELETE ON tenant_roles
FOR EACH ROW EXECUTE FUNCTION protect_system_role();

CREATE FUNCTION protect_owner_role_permissions()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_role_id UUID;
BEGIN
    EXECUTE format(
        'SELECT id FROM %I.tenant_roles WHERE code = ''OWNER_ADMIN''',
        TG_TABLE_SCHEMA
    ) INTO v_owner_role_id;

    IF TG_OP = 'DELETE' THEN
        IF OLD.role_id = v_owner_role_id THEN
            RAISE EXCEPTION 'OWNER_ADMIN permissions cannot be removed manually'
                USING ERRCODE = '23514';
        END IF;
        RETURN OLD;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.role_id = v_owner_role_id OR NEW.role_id = v_owner_role_id THEN
            RAISE EXCEPTION 'OWNER_ADMIN permissions cannot be modified manually'
                USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_owner_role_permissions
BEFORE UPDATE OR DELETE ON tenant_role_permissions
FOR EACH ROW EXECUTE FUNCTION protect_owner_role_permissions();

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
        RAISE EXCEPTION 'Tenant ownership cannot be modified by a generic operation'
            USING ERRCODE = '23514';
    END IF;

    IF OLD.owner_user_id IS NULL AND NEW.owner_user_id IS NOT NULL THEN
        EXECUTE format(
            'SELECT EXISTS (SELECT 1 FROM %I.tenant_users WHERE id = $1)',
            TG_TABLE_SCHEMA
        ) INTO v_owner_exists USING NEW.owner_user_id;

        IF v_owner_exists IS NOT TRUE THEN
            RAISE EXCEPTION 'Owner user does not exist'
                USING ERRCODE = '23503';
        END IF;
    END IF;

    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_protect_owner_state
BEFORE UPDATE OF owner_user_id ON tenant_authorization_state
FOR EACH ROW EXECUTE FUNCTION protect_owner_state();

CREATE FUNCTION protect_owner_user_role()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_role_id UUID;
    v_owner_user_id UUID;
BEGIN
    EXECUTE format(
        'SELECT id FROM %I.tenant_roles WHERE code = ''OWNER_ADMIN''',
        TG_TABLE_SCHEMA
    ) INTO v_owner_role_id;

    IF TG_OP = 'INSERT' THEN
        IF NEW.role_id = v_owner_role_id THEN
            EXECUTE format(
                'SELECT owner_user_id FROM %I.tenant_authorization_state WHERE id = 1 FOR UPDATE',
                TG_TABLE_SCHEMA
            ) INTO v_owner_user_id;

            IF v_owner_user_id IS NULL OR v_owner_user_id <> NEW.user_id THEN
                RAISE EXCEPTION 'OWNER_ADMIN can only be assigned to the registered owner'
                    USING ERRCODE = '23514';
            END IF;
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'UPDATE' THEN
        IF OLD.role_id = v_owner_role_id OR NEW.role_id = v_owner_role_id THEN
            RAISE EXCEPTION 'OWNER_ADMIN assignments cannot be modified'
                USING ERRCODE = '23514';
        END IF;
        RETURN NEW;
    END IF;

    IF TG_OP = 'DELETE' THEN
        IF OLD.role_id = v_owner_role_id THEN
            RAISE EXCEPTION 'OWNER_ADMIN assignment cannot be removed'
                USING ERRCODE = '23514';
        END IF;
        RETURN OLD;
    END IF;

    RETURN NULL;
END;
$$;

CREATE TRIGGER trg_protect_owner_user_role
BEFORE INSERT OR UPDATE OR DELETE ON tenant_user_roles
FOR EACH ROW EXECUTE FUNCTION protect_owner_user_role();

CREATE FUNCTION assert_owner_assignment_consistency()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = pg_catalog
AS $$
DECLARE
    v_owner_user_id UUID;
    v_has_assignment BOOLEAN;
BEGIN
    EXECUTE format(
        'SELECT owner_user_id FROM %I.tenant_authorization_state WHERE id = 1',
        TG_TABLE_SCHEMA
    ) INTO v_owner_user_id;

    IF v_owner_user_id IS NULL THEN
        RETURN NULL;
    END IF;

    EXECUTE format(
        'SELECT EXISTS (
            SELECT 1
            FROM %I.tenant_user_roles tur
            JOIN %I.tenant_roles r ON r.id = tur.role_id
            WHERE r.code = ''OWNER_ADMIN'' AND tur.user_id = $1
        )',
        TG_TABLE_SCHEMA,
        TG_TABLE_SCHEMA
    ) INTO v_has_assignment USING v_owner_user_id;

    IF v_has_assignment IS NOT TRUE THEN
        RAISE EXCEPTION 'Tenant owner has no OWNER_ADMIN assignment'
            USING ERRCODE = '23514';
    END IF;

    RETURN NULL;
END;
$$;

CREATE CONSTRAINT TRIGGER trg_assert_owner_assignment
AFTER INSERT OR UPDATE ON tenant_authorization_state
DEFERRABLE INITIALLY DEFERRED FOR EACH ROW
EXECUTE FUNCTION assert_owner_assignment_consistency();


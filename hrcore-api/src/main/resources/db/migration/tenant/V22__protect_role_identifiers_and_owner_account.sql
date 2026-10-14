-- El código de un rol no debe invadir las identidades reservadas.

ALTER TABLE tenant_roles
    ADD CONSTRAINT ck_tenant_roles_reserved_custom_codes
    CHECK (
        code = 'OWNER_ADMIN'
        OR (
            code <> 'SUPERADMIN'
            AND LEFT(code, 5) <> 'ROLE_'
        )
    );

-- La identidad del propietario se consulta en el mismo esquema
-- que contiene la fila que activó el trigger.

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
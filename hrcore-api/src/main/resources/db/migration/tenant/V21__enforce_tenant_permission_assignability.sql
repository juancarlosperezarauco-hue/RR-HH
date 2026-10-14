CREATE FUNCTION enforce_tenant_permission_assignment()
RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE v_system_role BOOLEAN; v_scope VARCHAR(20); v_assignable BOOLEAN; v_active BOOLEAN;
BEGIN
    SELECT system_role INTO v_system_role FROM tenant_roles WHERE id = NEW.role_id;
    IF NOT FOUND THEN RAISE EXCEPTION 'Tenant role does not exist' USING ERRCODE = '23503'; END IF;
    SELECT scope, custom_role_assignable, active INTO v_scope, v_assignable, v_active
    FROM public.system_permissions WHERE code = NEW.permission_code;
    IF NOT FOUND THEN RAISE EXCEPTION 'System permission does not exist' USING ERRCODE = '23503'; END IF;
    IF v_scope <> 'TENANT' THEN RAISE EXCEPTION 'Platform permissions cannot be assigned to tenant roles' USING ERRCODE = '23514'; END IF;
    IF v_active IS NOT TRUE THEN RAISE EXCEPTION 'Permission is inactive' USING ERRCODE = '23514'; END IF;
    IF v_system_role IS FALSE AND v_assignable IS NOT TRUE THEN RAISE EXCEPTION 'Reserved permission cannot be assigned to custom roles' USING ERRCODE = '23514'; END IF;
    RETURN NEW;
END; $$;
CREATE TRIGGER trg_enforce_tenant_permission_assignment BEFORE INSERT OR UPDATE ON tenant_role_permissions
FOR EACH ROW EXECUTE FUNCTION enforce_tenant_permission_assignment();

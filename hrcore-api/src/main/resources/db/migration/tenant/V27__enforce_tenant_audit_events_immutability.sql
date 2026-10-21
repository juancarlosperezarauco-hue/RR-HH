-- ============================================================
-- V25: INMUTABILIDAD DE EVENTOS DE AUDITORÍA DEL TENANT
-- ============================================================

CREATE OR REPLACE FUNCTION enforce_tenant_audit_events_immutability()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF TG_OP = 'UPDATE' THEN
        RAISE EXCEPTION 'Audit events are immutable and cannot be updated'
            USING ERRCODE = '23514';
    END IF;

    IF TG_OP = 'DELETE' THEN
        RAISE EXCEPTION 'Audit events are immutable and cannot be deleted'
            USING ERRCODE = '23514';
    END IF;

    RETURN NULL;
END;
$$;

DROP TRIGGER IF EXISTS trg_enforce_tenant_audit_events_immutability ON tenant_audit_events;

CREATE TRIGGER trg_enforce_tenant_audit_events_immutability
BEFORE UPDATE OR DELETE ON tenant_audit_events
FOR EACH ROW
EXECUTE FUNCTION enforce_tenant_audit_events_immutability();

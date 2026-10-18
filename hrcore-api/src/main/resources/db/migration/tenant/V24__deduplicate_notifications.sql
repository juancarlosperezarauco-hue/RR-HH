-- Clave estable para evitar notificaciones laborales duplicadas en reintentos concurrentes.
CREATE UNIQUE INDEX IF NOT EXISTS uq_tenant_notifications_user_action
    ON tenant_notifications(user_id, action_url)
    WHERE action_url IS NOT NULL;

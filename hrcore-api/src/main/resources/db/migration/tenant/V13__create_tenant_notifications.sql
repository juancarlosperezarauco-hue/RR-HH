CREATE TABLE IF NOT EXISTS tenant_notification_devices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    device_id VARCHAR(120) NOT NULL,
    fcm_token TEXT NOT NULL,
    platform VARCHAR(20) NOT NULL,
    device_name VARCHAR(150),
    app_version VARCHAR(40),
    os_version VARCHAR(40),
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_seen_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tenant_notification_devices_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_tenant_notification_devices_user_device
        UNIQUE (user_id, device_id),

    CONSTRAINT uq_tenant_notification_devices_token
        UNIQUE (fcm_token),

    CONSTRAINT chk_tenant_notification_devices_platform
        CHECK (platform IN ('ANDROID', 'IOS', 'WEB', 'DESKTOP', 'UNKNOWN')),

    CONSTRAINT chk_tenant_notification_devices_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'EXPIRED', 'REVOKED'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_devices_user_id
    ON tenant_notification_devices(user_id);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_devices_status
    ON tenant_notification_devices(status);

CREATE TABLE IF NOT EXISTS tenant_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(60) NOT NULL,
    category VARCHAR(30) NOT NULL,
    priority VARCHAR(20) NOT NULL,
    title VARCHAR(255) NOT NULL,
    body TEXT NOT NULL,
    data JSONB NULL,
    image_url TEXT NULL,
    action_url TEXT NULL,
    read_at TIMESTAMPTZ NULL,
    opened_at TIMESTAMPTZ NULL,
    archived_at TIMESTAMPTZ NULL,
    expires_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tenant_notifications_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT chk_tenant_notifications_category
        CHECK (category IN (
            'TRANSACTIONS',
            'PAYMENTS',
            'ACCOUNTS',
            'SECURITY',
            'SYSTEM',
            'FX',
            'LIMITS',
            'ADMIN'
        )),

    CONSTRAINT chk_tenant_notifications_priority
        CHECK (priority IN ('LOW', 'NORMAL', 'HIGH', 'CRITICAL'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_notifications_user_id
    ON tenant_notifications(user_id);

CREATE INDEX IF NOT EXISTS idx_tenant_notifications_type
    ON tenant_notifications(type);

CREATE INDEX IF NOT EXISTS idx_tenant_notifications_category
    ON tenant_notifications(category);

CREATE INDEX IF NOT EXISTS idx_tenant_notifications_created_at
    ON tenant_notifications(created_at DESC);

CREATE TABLE IF NOT EXISTS tenant_notification_deliveries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    device_id UUID NULL,
    channel VARCHAR(20) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    provider_message_id VARCHAR(120),
    error_code VARCHAR(80),
    error_message TEXT,
    attempted_at TIMESTAMPTZ NULL,
    sent_at TIMESTAMPTZ NULL,
    failed_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tenant_notification_deliveries_notification
        FOREIGN KEY (notification_id)
        REFERENCES tenant_notifications(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_tenant_notification_deliveries_device
        FOREIGN KEY (device_id)
        REFERENCES tenant_notification_devices(id)
        ON DELETE SET NULL,

    CONSTRAINT chk_tenant_notification_deliveries_channel
        CHECK (channel IN ('PUSH', 'IN_APP', 'EMAIL', 'SMS')),

    CONSTRAINT chk_tenant_notification_deliveries_status
        CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED', 'EXPIRED'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_deliveries_notification_id
    ON tenant_notification_deliveries(notification_id);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_deliveries_device_id
    ON tenant_notification_deliveries(device_id);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_deliveries_status
    ON tenant_notification_deliveries(status);

CREATE TABLE IF NOT EXISTS tenant_notification_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    category VARCHAR(30) NOT NULL,
    push_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_tenant_notification_preferences_user
        FOREIGN KEY (user_id)
        REFERENCES tenant_users(id)
        ON DELETE RESTRICT,

    CONSTRAINT uq_tenant_notification_preferences_user_category
        UNIQUE (user_id, category),

    CONSTRAINT chk_tenant_notification_preferences_category
        CHECK (category IN (
            'TRANSACTIONS',
            'PAYMENTS',
            'ACCOUNTS',
            'SECURITY',
            'SYSTEM',
            'FX',
            'LIMITS',
            'ADMIN'
        ))
);

CREATE TABLE IF NOT EXISTS tenant_notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    type VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL,
    title_template VARCHAR(255) NOT NULL,
    body_template TEXT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_tenant_notification_templates_type_channel
        UNIQUE (type, channel),

    CONSTRAINT chk_tenant_notification_templates_channel
        CHECK (channel IN ('PUSH', 'IN_APP', 'EMAIL', 'SMS'))
);

CREATE INDEX IF NOT EXISTS idx_tenant_notification_templates_type
    ON tenant_notification_templates(type);

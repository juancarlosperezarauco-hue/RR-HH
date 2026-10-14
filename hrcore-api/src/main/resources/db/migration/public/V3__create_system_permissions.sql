CREATE TABLE public.system_permissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    code VARCHAR(100) NOT NULL UNIQUE,
    module VARCHAR(50) NOT NULL,
    description VARCHAR(255),

    scope VARCHAR(20) NOT NULL DEFAULT 'TENANT',
    custom_role_assignable BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT ck_system_permissions_scope
        CHECK (scope IN ('TENANT', 'PLATFORM')),

    CONSTRAINT ck_system_permissions_code
        CHECK (code ~ '^[a-z][a-z0-9_-]*(\.[a-z0-9_-]+)*$'),

    CONSTRAINT ck_platform_permission_assignment
        CHECK (scope <> 'PLATFORM' OR custom_role_assignable = FALSE)
);

CREATE INDEX idx_system_permissions_scope
    ON public.system_permissions (scope);

CREATE INDEX idx_system_permissions_active
    ON public.system_permissions (active);

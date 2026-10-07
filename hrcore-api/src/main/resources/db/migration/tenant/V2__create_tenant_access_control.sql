CREATE TABLE IF NOT EXISTS tenant_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    description VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS tenant_user_roles (
    user_id UUID NOT NULL,
    role_id UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_tenant_user_roles_user
        FOREIGN KEY (user_id) REFERENCES tenant_users(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_user_roles_role
        FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS tenant_role_permissions (
    role_id UUID NOT NULL,
    permission_code VARCHAR(100) NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (role_id, permission_code),
    CONSTRAINT fk_tenant_role_permissions_role
        FOREIGN KEY (role_id) REFERENCES tenant_roles(id) ON DELETE CASCADE
);
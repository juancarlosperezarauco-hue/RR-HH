ALTER TABLE public.platform_tenants
    ADD COLUMN IF NOT EXISTS maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS maintenance_reason TEXT NULL,
    ADD COLUMN IF NOT EXISTS maintenance_started_at TIMESTAMPTZ NULL;

CREATE TABLE IF NOT EXISTS public.backup_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    operation_type VARCHAR(30) NOT NULL,
    scope VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    tenant_id UUID NULL REFERENCES public.platform_tenants(id) ON DELETE SET NULL,
    tenant_slug VARCHAR(100) NULL,
    schema_name VARCHAR(128) NULL,
    source_backup_id UUID NULL REFERENCES public.backup_jobs(id) ON DELETE SET NULL,
    pre_restore_backup_id UUID NULL REFERENCES public.backup_jobs(id) ON DELETE SET NULL,
    file_name VARCHAR(255) NOT NULL,
    storage_path TEXT NOT NULL,
    format VARCHAR(30) NOT NULL DEFAULT 'CUSTOM',
    size_bytes BIGINT NULL,
    checksum_sha256 VARCHAR(128) NULL,
    requested_by VARCHAR(150),
    reason TEXT NULL,
    failure_reason TEXT NULL,
    started_at TIMESTAMPTZ NULL,
    finished_at TIMESTAMPTZ NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_backup_jobs_operation_type CHECK (
        operation_type IN ('BACKUP', 'RESTORE')
    ),

    CONSTRAINT chk_backup_jobs_scope CHECK (
        scope IN ('TENANT_SCHEMA', 'FULL_DATABASE')
    ),

    CONSTRAINT chk_backup_jobs_status CHECK (
        status IN (
            'PENDING',
            'RUNNING',
            'COMPLETED',
            'FAILED',
            'RESTORING',
            'RESTORED',
            'RESTORED_WITH_WARNINGS',
            'RESTORE_FAILED'
        )
    ),

    CONSTRAINT chk_backup_jobs_format CHECK (
        format IN ('CUSTOM', 'SQL')
    )
);

CREATE INDEX IF NOT EXISTS idx_backup_jobs_tenant_slug
    ON public.backup_jobs(tenant_slug);

CREATE INDEX IF NOT EXISTS idx_backup_jobs_status
    ON public.backup_jobs(status);

CREATE INDEX IF NOT EXISTS idx_backup_jobs_created_at
    ON public.backup_jobs(created_at DESC);

CREATE INDEX IF NOT EXISTS idx_backup_jobs_scope
    ON public.backup_jobs(scope);

CREATE UNIQUE INDEX IF NOT EXISTS uq_backup_jobs_active_per_tenant
    ON public.backup_jobs(tenant_slug)
    WHERE tenant_slug IS NOT NULL
      AND status IN ('PENDING', 'RUNNING', 'RESTORING');

CREATE UNIQUE INDEX IF NOT EXISTS uq_backup_jobs_active_full_database
    ON public.backup_jobs(scope)
    WHERE scope = 'FULL_DATABASE'
      AND status IN ('PENDING', 'RUNNING', 'RESTORING');

INSERT INTO public.system_permissions (code, module, description, active, created_at)
VALUES
    ('backups.create', 'BACKUPS', 'Create tenant backups', true, NOW()),
    ('backups.list', 'BACKUPS', 'List tenant backups', true, NOW()),
    ('backups.detail', 'BACKUPS', 'View tenant backup detail', true, NOW()),
    ('backups.download', 'BACKUPS', 'Download tenant backup artifact', true, NOW()),
    ('backups.restore', 'BACKUPS', 'Restore tenant backup', true, NOW())
ON CONFLICT (code) DO NOTHING;

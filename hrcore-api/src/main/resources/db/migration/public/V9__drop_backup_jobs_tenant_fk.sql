ALTER TABLE public.backup_jobs
    DROP CONSTRAINT IF EXISTS backup_jobs_tenant_id_fkey;

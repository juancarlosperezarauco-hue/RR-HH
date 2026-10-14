-- Legacy migration intentionally kept as a no-op.
-- OWNER_ADMIN permissions are synchronized by OwnerPermissionSynchronizer.
-- Individual permissions must not be granted through tenant migrations.
SELECT 1;

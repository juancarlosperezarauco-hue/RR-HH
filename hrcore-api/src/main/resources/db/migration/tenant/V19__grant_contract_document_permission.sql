-- Permite al administrador propietario descargar e imprimir contratos.
INSERT INTO tenant_role_permissions (role_id, permission_code, assigned_at)
SELECT r.id, 'contracts.document.download', NOW()
FROM tenant_roles r
JOIN public.system_permissions p
  ON p.code = 'contracts.document.download' AND p.active = TRUE
WHERE r.name = 'OWNER_ADMIN'
ON CONFLICT (role_id, permission_code) DO NOTHING;

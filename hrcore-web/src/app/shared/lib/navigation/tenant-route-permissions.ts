export const TENANT_ROUTE_PERMISSIONS = {
  users: 'users.list',
  roles: 'access.roles.read',
  permissions: 'access.permissions.read',
  subscription: 'billing.subscription.manage',
  backups: 'backups.list',
  employees: 'employees.list',
  contracts: 'contracts.list',
  departments: 'departments.list',
  positions: 'positions.list',
} as const;

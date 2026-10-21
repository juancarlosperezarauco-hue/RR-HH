export interface CreateTenantUserRequest {
  email: string;
  password?: string;
  firstName?: string | null;
  lastName?: string | null;
  displayName?: string | null;
  status?: 'ACTIVE' | 'PENDING' | 'INACTIVE';
  employeeId?: string | null;
  roleIds?: string[];
}

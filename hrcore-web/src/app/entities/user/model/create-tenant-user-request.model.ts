export interface CreateTenantUserRequest {
  email: string;
  password: string;
  firstName?: string | null;
  lastName?: string | null;
  roleIds?: string[];
}

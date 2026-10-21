export interface UpdateTenantUserRequest {
  email: string;
  firstName?: string | null;
  lastName?: string | null;
  displayName?: string | null;
  employeeId?: string | null;
}

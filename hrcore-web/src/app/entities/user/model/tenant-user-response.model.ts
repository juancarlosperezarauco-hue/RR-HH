export interface TenantUserResponse {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  displayName?: string | null;
  active: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
  roles: AssignedRoleResponse[];
  employeeId?: string | null;
  employeeNumber?: string | null;
  employeeFullName?: string | null;
  owner?: boolean;
}

export interface AssignedRoleResponse {
  id: string;
  code: string;
  name: string;
  active: boolean;
}

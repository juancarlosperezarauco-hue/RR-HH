export interface TenantUserResponse {
  id: string;
  email: string;
  firstName: string | null;
  lastName: string | null;
  active: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
  roles: AssignedRoleResponse[];
}

export interface AssignedRoleResponse { id: string; code: string; name: string; active: boolean; }

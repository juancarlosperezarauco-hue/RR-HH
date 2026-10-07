import { TenantRoleResponse } from './tenant-role.model';

export interface UserRolesResponse {
  userId: string;
  roles: TenantRoleResponse[];
}

export interface AssignUserRolesRequest {
  roleIds: string[];
}

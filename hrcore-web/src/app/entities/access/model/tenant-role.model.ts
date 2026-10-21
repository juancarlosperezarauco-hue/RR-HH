export type TenantRoleType = 'ROOT' | 'INTERNAL';

export interface TenantRoleResponse {
  id: string;
  code: string;
  name: string;
  description: string | null;
  roleType: TenantRoleType;
  systemRole: boolean;
  active: boolean;
  createdAt: string;
  updatedAt: string;
  permissionCodes: string[];
}

export interface CreateTenantRoleRequest {
  code?: string;
  name: string;
  description?: string | null;
  permissionCodes: string[];
}

export interface UpdateTenantRoleRequest {
  name: string;
  description?: string | null;
  permissionCodes: string[];
}

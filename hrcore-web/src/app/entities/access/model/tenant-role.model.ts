export interface TenantRoleResponse {
  id: string;
  name: string;
  description: string;
  active: boolean;
  createdAt: string;
  permissionCodes: string[];
}

export interface CreateTenantRoleRequest {
  name: string;
  description?: string;
  permissionCodes: string[];
}

export interface UpdateTenantRoleRequest {
  name: string;
  description?: string;
  permissionCodes: string[];
}

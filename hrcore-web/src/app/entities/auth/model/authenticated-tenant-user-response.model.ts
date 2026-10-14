export interface AuthenticatedTenantUserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  status: string;
  tenantSlug: string;
  scope: 'TENANT';
  owner: boolean;
  roles: string[];
  roleDetails: AuthenticatedRoleResponse[];
  permissions: string[];
  securityVersion: number;
  authorizationVersion: number;
  catalogVersion: number;
  profilePhotoUrl?: string | null;
  profilePhotoContentType?: string | null;
  updatedAt?: string | null;
}

export interface AuthenticatedRoleResponse {
  id: string;
  code: string;
  name: string;
  systemRole: boolean;
}

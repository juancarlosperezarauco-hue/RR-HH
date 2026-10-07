export interface AuthenticatedTenantUserResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  status: string;
  tenantSlug: string;
  roles: string[];
  profilePhotoUrl?: string | null;
  profilePhotoContentType?: string | null;
  updatedAt?: string | null;
}

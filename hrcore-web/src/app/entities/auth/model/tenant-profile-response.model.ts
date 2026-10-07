export interface TenantProfileResponse {
  id: string;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  status: string;
  tenantSlug: string;
  faceLoginEnabled: boolean;
  profilePhotoAvailable: boolean;
  profilePhotoUrl: string | null;
  profilePhotoContentType: string | null;
  createdAt: string | null;
  updatedAt: string | null;
}

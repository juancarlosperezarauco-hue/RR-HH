export interface PublicSignupRequest {
  companyName: string;
  tenantSlug: string;
  adminEmail: string;
  password?: string;
  firstName: string;
  lastName: string;
}

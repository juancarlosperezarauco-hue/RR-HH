export interface PublicSignupResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  adminUserId: string;
  adminEmail: string;
  planCode: string;
  message?: string;
}

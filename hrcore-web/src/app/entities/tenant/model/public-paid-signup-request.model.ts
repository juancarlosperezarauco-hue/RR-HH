export interface PublicPaidSignupRequest {
  companyName: string;
  tenantSlug: string;
  adminEmail: string;
  password?: string;
  firstName: string;
  lastName: string;
  planCode: string;
  billingInterval: 'MONTHLY' | 'YEARLY';
}

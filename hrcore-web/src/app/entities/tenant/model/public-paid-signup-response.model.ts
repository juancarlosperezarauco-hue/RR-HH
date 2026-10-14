export interface PublicPaidSignupResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  adminEmail: string;
  ownerRole: string;
  initialPlanCode: string;
  selectedPlanCode: string;
  billingInterval: 'MONTHLY' | 'YEARLY';
  checkoutSessionId: string;
  checkoutUrl: string;
  checkoutStatus: string;
  checkoutExpiresAt: string | null;
  message: string;
}

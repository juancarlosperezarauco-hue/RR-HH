export interface PublicPaidSignupResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  adminEmail: string;
  initialPlanCode: string;
  selectedPlanCode: string;
  billingInterval: string;
  checkoutSessionId: string | null;
  checkoutUrl: string | null;
  checkoutStatus: string;
  expiresAt: string | null;
  message?: string;
}

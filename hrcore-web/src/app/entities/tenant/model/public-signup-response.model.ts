export interface PublicSignupResponse {
  tenantId: string;
  tenantSlug: string;
  companyName: string;
  adminEmail: string;
  initialRole: string;
  currentPlanCode: string;
  subscriptionStatus: string;
  trialExpiresAt: string | null;
  loginHint: string;
}

export interface PendingCheckout {
  tenantSlug: string;
  companyName: string;
  adminEmail: string;
  requestedPlanCode: string;
  selectedPlanCode: string;
  billingInterval: 'MONTHLY' | 'YEARLY' | string;
  checkoutSessionId: string;
  checkoutUrl?: string;
  createdAt: string;
}

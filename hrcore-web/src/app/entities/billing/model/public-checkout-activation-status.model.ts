export type CheckoutActivationStatus = 'PENDING' | 'ACTIVE' | 'FAILED' | string;

export interface PublicCheckoutActivationStatusResponse {
  checkoutSessionId: string;
  stripeSessionId: string;
  tenantId: string;
  tenantSlug: string;
  planCode: string;
  billingInterval: 'MONTHLY' | 'YEARLY' | string;
  checkoutStatus: string;
  activationStatus: CheckoutActivationStatus;
  paid: boolean;
  active: boolean;
  failed: boolean;
  pending: boolean;
  message: string;
  expiresAt: string | null;
}

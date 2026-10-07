export type BillingInterval = 'MONTHLY' | 'YEARLY';

export interface TenantBillingPlan {
  id: string;
  code: string;
  name: string;
  description: string | null;
  planType: string;
  maxUsers: number;
  maxRoles: number;
  trialDays: number | null;
  monthlyAmount: number | null;
  yearlyAmount: number | null;
  currency: string;
  publicVisible: boolean;
  sortOrder: number;
  active: boolean;
  currentPlan: boolean;
  availableForCheckout: boolean;
  contactSales: boolean;
}

export interface TenantCheckoutSessionRequest {
  planCode: string;
  billingInterval: BillingInterval;
}

export interface TenantCheckoutSessionResponse {
  id: string;
  stripeSessionId: string;
  checkoutUrl: string;
  status: string;
  selectedPlanCode: string;
  billingInterval: BillingInterval;
  expiresAt: string | null;
}

export interface TenantCheckoutResultResponse {
  checkoutSessionId: string;
  stripeSessionId: string;
  status: string;
  planCode: string;
  billingInterval: BillingInterval | string;
  checkoutUrl: string;
  completedAt: string | null;
  expiresAt: string | null;
}

export interface TenantCheckoutActivationStatusResponse {
  checkoutSessionId: string;
  stripeSessionId: string;
  tenantSlug: string | null;
  planCode: string;
  billingInterval: BillingInterval | string;
  checkoutStatus: string;
  activationStatus: 'PENDING' | 'ACTIVE' | 'FAILED' | string;
  paid: boolean;
  active: boolean;
  failed: boolean;
  pending: boolean;
  message: string;
  expiresAt: string | null;
}

export interface TenantUpgradeCheckoutPending {
  stripeSessionId: string;
  selectedPlanCode: string;
  billingInterval: BillingInterval;
  checkoutUrl: string;
  createdAt: string;
}

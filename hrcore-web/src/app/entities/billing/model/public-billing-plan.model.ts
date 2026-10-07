export interface PublicBillingPlan {
  id: string;
  code: string;
  name: string;
  description: string | null;
  planType: 'DEMO' | 'PAID' | 'ENTERPRISE' | string;
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

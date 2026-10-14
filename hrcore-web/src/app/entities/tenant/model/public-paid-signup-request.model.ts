import { PublicSignupRequest } from './public-signup-request.model';

export interface PublicPaidSignupRequest extends PublicSignupRequest {
  planCode: string;
  billingInterval: 'MONTHLY' | 'YEARLY';
}

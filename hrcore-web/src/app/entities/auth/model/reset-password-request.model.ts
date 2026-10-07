export interface ResetPasswordRequest {
  tenantSlug: string;
  token: string;
  newPassword: string;
}

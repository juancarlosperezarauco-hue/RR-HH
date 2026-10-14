export type AuthScope = 'TENANT' | 'PLATFORM';
export interface StoredAuthSession {
  scope: AuthScope;
  tenantSlug: string | null;
  accessToken: string;
  refreshToken: string;
  accessExpiresAt: number;
  revision: string;
  loginId: string;
}

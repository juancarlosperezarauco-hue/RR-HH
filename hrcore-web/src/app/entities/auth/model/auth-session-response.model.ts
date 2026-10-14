export interface AuthSessionResponse {
  id: string;
  current: boolean;
  createdAt: string;
  lastUsedAt: string | null;
  expiresAt: string;
  revoked: boolean;
  userAgent: string | null;
  ipAddress: string | null;
}
export interface SessionMutationResponse { status: string; }
export type SessionScope = 'TENANT' | 'PLATFORM';
export type SessionDisplayStatus = 'CURRENT' | 'ACTIVE' | 'EXPIRED' | 'REVOKED';
export function deriveSessionStatus(session: AuthSessionResponse, now: number): SessionDisplayStatus {
  if (session.revoked) return 'REVOKED';
  if (!Number.isFinite(Date.parse(session.expiresAt))) return 'EXPIRED';
  if (Date.parse(session.expiresAt) <= now) return 'EXPIRED';
  if (session.current) return 'CURRENT';
  return 'ACTIVE';
}

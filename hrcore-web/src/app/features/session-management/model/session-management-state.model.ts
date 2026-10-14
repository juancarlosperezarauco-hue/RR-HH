import { AuthSessionResponse, SessionScope } from '../../../entities/auth';
export type SessionLoadStatus = 'idle' | 'loading' | 'ready' | 'error';
export type SessionMutationKind = 'revoke' | 'revoke-others' | 'revoke-current';
export interface SessionManagementState { status: SessionLoadStatus; sessions: AuthSessionResponse[]; error: string | null; pendingAction: SessionMutationKind | null; pendingSessionId: string | null; mutationError: string | null; updatedAt: number | null; }
export interface SessionContext { scope: SessionScope; loginId: string; tenantSlug: string | null; revision: string; }

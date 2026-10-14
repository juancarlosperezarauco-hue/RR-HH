import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { AuthSessionResponse, SessionScope } from '../../../entities/auth';
import { TenantSessionService } from '../../../entities/auth/api/tenant-session.service';
import { PlatformSessionService } from '../../../entities/platform/api/platform-session.service';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
import { PlatformStorageService } from '../../platform/lib/platform-storage.service';
import { SessionManagementState } from '../model/session-management-state.model';
import { AuthFacade } from '../../../shared/lib/auth/auth.facade';
import { PlatformSuperadminMeUseCase } from '../../platform/application/platform-superadmin-me.usecase';

@Injectable({ providedIn: 'root' })
export class SessionManagementFacade {
  private readonly tenantApi = inject(TenantSessionService);
  private readonly platformApi = inject(PlatformSessionService);
  private readonly coordinator = inject(SessionCoordinatorService);
  private readonly tenantStorage = inject(AuthStorageService);
  private readonly platformStorage = inject(PlatformStorageService);
  private readonly router = inject(Router);
  private readonly authFacade = inject(AuthFacade);
  private readonly platformMe = inject(PlatformSuperadminMeUseCase);
  private readonly states = new Map<SessionScope, ReturnType<typeof signal<SessionManagementState>>>([
    ['TENANT', signal(this.initialState())], ['PLATFORM', signal(this.initialState())]
  ]);
  private readonly sequences = new Map<SessionScope, number>([['TENANT', 0], ['PLATFORM', 0]]);
  readonly tenantState = computed(() => this.states.get('TENANT')!());
  readonly platformState = computed(() => this.states.get('PLATFORM')!());

  async load(scope: SessionScope, force = false): Promise<void> {
    const state = this.states.get(scope)!; if (!force && state().status === 'ready') return;
    const context = this.context(scope); if (!context) { state.set({ ...this.initialState(), status: 'error', error: 'La sesión ya no está disponible.' }); return; }
    const sequence = (this.sequences.get(scope) ?? 0) + 1; this.sequences.set(scope, sequence);
    state.set({ ...state(), status: 'loading', error: null });
    try {
      const sessions = scope === 'TENANT' ? await firstValueFrom(this.tenantApi.list()) : await firstValueFrom(this.platformApi.list());
      if (!this.sameContext(scope, context) || sequence !== this.sequences.get(scope)) return;
      state.set({ ...state(), status: 'ready', sessions: this.sort(sessions), error: null, updatedAt: Date.now() });
    } catch (err: any) {
      if (!this.sameContext(scope, context) || sequence !== this.sequences.get(scope)) return;
      state.set({ ...state(), status: 'error', error: err.error?.message || err.message || 'No se pudieron cargar las sesiones.' });
    }
  }

  async revokeOtherSession(scope: SessionScope, sessionId: string): Promise<void> { await this.mutate(scope, 'revoke', sessionId, async () => scope === 'TENANT' ? firstValueFrom(this.tenantApi.revoke(sessionId)) : firstValueFrom(this.platformApi.revoke(sessionId))); }
  async revokeOtherSessions(scope: SessionScope): Promise<void> { await this.mutate(scope, 'revoke-others', null, async () => scope === 'TENANT' ? firstValueFrom(this.tenantApi.revokeOthers()) : firstValueFrom(this.platformApi.revokeOthers())); }
  async revokeCurrentSession(scope: SessionScope, sessionId: string): Promise<void> {
    const state = this.states.get(scope)!; state.set({ ...state(), pendingAction: 'revoke-current', pendingSessionId: sessionId, mutationError: null });
    const result = await this.coordinator.closeCurrentSession(scope, sessionId);
    if (result.revokedRemotely) {
      state.set({ ...this.initialState(), status: 'idle' });
      if (scope === 'TENANT') { this.authFacade.currentUser.set(null); this.authFacade.status.set('idle'); await this.router.navigate(['/login'], { replaceUrl: true }); }
      else { this.platformMe.resetState(); await this.router.navigate(['/platform/login'], { replaceUrl: true }); }
      return;
    }
    state.set({ ...state(), pendingAction: null, pendingSessionId: null, mutationError: result.reason === 'network-error' ? 'No se pudo confirmar el cierre de la sesión.' : 'No se pudo cerrar la sesión.' });
  }
  reset(scope: SessionScope): void { this.sequences.set(scope, (this.sequences.get(scope) ?? 0) + 1); this.states.get(scope)!.set(this.initialState()); }
  private async mutate(scope: SessionScope, action: 'revoke' | 'revoke-others', sessionId: string | null, operation: () => Promise<void>): Promise<void> {
    const state = this.states.get(scope)!; const context = this.context(scope); if (!context) return;
    if (state().pendingAction) return; state.set({ ...state(), pendingAction: action, pendingSessionId: sessionId, mutationError: null });
    try { await operation(); if (this.sameContext(scope, context)) { state.set({ ...state(), pendingAction: null, pendingSessionId: null }); await this.load(scope, true); } }
    catch (err: any) { if (this.sameContext(scope, context)) state.set({ ...state(), pendingAction: null, pendingSessionId: null, mutationError: err.error?.message || err.message || 'No se pudo completar la operación.' }); }
  }
  private context(scope: SessionScope) { const session = scope === 'TENANT' ? this.tenantStorage.getSession() : this.platformStorage.getSession(); return session ? { scope, loginId: session.loginId, tenantSlug: session.tenantSlug, revision: session.revision } : null; }
  private sameContext(scope: SessionScope, context: any): boolean { const current = this.context(scope); return !!current && current.loginId === context.loginId && current.tenantSlug === context.tenantSlug; }
  private sort(sessions: AuthSessionResponse[]): AuthSessionResponse[] { const now = Date.now(); return [...sessions].sort((a, b) => Number(b.current) - Number(a.current) || Number(Date.parse(b.createdAt) <= now && Date.parse(a.createdAt) > now) - Number(Date.parse(a.createdAt) <= now && Date.parse(b.createdAt) > now) || Date.parse(b.createdAt) - Date.parse(a.createdAt)); }
  private initialState(): SessionManagementState { return { status: 'idle', sessions: [], error: null, pendingAction: null, pendingSessionId: null, mutationError: null, updatedAt: null }; }
}

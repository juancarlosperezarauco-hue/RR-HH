import { Component, OnInit, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { SessionListComponent, SessionManagementFacade } from '../../features/session-management';
import { AuthSessionResponse } from '../../entities/auth';

@Component({ selector: 'app-tenant-security-page', standalone: true, imports: [RouterModule, SessionListComponent], template: `<div class="space-y-6 p-4 sm:p-6 lg:p-8"><section class="rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-lg"><h1 class="text-3xl font-black text-accent-foreground">Seguridad de la cuenta</h1><p class="mt-2 text-sm text-muted-foreground">Administra las sesiones activas de tu cuenta en esta organización.</p><a routerLink="/dashboard/profile" class="mt-4 inline-flex rounded-full border border-border px-4 py-2 text-sm font-semibold text-primary">Volver a mi perfil</a></section><app-session-list [scope]="'TENANT'" [sessions]="state().sessions" [status]="state().status" [error]="state().error" [pendingAction]="state().pendingAction" [pendingSessionId]="state().pendingSessionId" [mutationError]="state().mutationError" (refreshRequested)="load()" (revokeRequested)="revoke($event)" (revokeOthersRequested)="revokeOthers()" /></div>` })
export class TenantSecurityPageComponent implements OnInit {
  readonly facade = inject(SessionManagementFacade); readonly state = this.facade.tenantState;
  ngOnInit(): void { void this.load(); }
  load(): Promise<void> { return this.facade.load('TENANT'); }
  async revoke(sessionId: string): Promise<void> { const session = this.state().sessions.find(item => item.id === sessionId); if (!session || !confirm(session.current ? 'Cerrar esta sesión requerirá iniciar sesión nuevamente. ¿Continuar?' : '¿Cerrar esta sesión?')) return; if (session.current) await this.facade.revokeCurrentSession('TENANT', sessionId); else await this.facade.revokeOtherSession('TENANT', sessionId); }
  async revokeOthers(): Promise<void> { if (confirm('Se cerrarán las demás sesiones de tu cuenta. ¿Continuar?')) await this.facade.revokeOtherSessions('TENANT'); }
}

import { CommonModule, DatePipe } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { AuthSessionResponse, SessionDisplayStatus, SessionScope, deriveSessionStatus } from '../../../../entities/auth';
import { SessionLoadStatus, SessionMutationKind } from '../..';

@Component({
  selector: 'app-session-list', standalone: true, imports: [CommonModule, DatePipe],
  template: `
    <div class="space-y-4">
      <div class="flex flex-wrap items-center justify-between gap-3"><div><h2 class="text-lg font-bold text-accent-foreground">Sesiones {{ scope === 'PLATFORM' ? 'de plataforma' : 'de tu cuenta' }}</h2><p class="text-sm text-muted-foreground">Puedes revisar y cerrar sesiones propias.</p></div><div class="flex gap-2"><button type="button" (click)="refreshRequested.emit()" [disabled]="status === 'loading'" class="rounded-full border border-border bg-card px-4 py-2 text-sm font-semibold text-primary">{{ status === 'loading' ? 'Actualizando...' : 'Actualizar sesiones' }}</button><button type="button" *ngIf="canRevokeOthers" (click)="revokeOthersRequested.emit()" [disabled]="pendingAction !== null" class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">Cerrar las demás</button></div></div>
      @if (error) { <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">{{ error }} <button class="ml-2 font-semibold underline" (click)="refreshRequested.emit()">Reintentar</button></div> }
      @if (mutationError) { <div class="rounded-2xl border border-destructive/20 bg-destructive/5 p-4 text-sm text-destructive">{{ mutationError }}</div> }
      @if (status === 'loading' && sessions.length === 0) { <div class="rounded-2xl border border-border bg-card p-8 text-sm text-muted-foreground">Cargando sesiones...</div> }
      @if (status === 'ready' && sessions.length === 0) { <div class="rounded-2xl border border-dashed border-border bg-card p-8 text-center text-sm text-muted-foreground">No hay sesiones para mostrar.</div> }
      @for (session of sessions; track session.id) {
        <article class="rounded-2xl border border-border bg-card p-5 shadow-sm"><div class="flex flex-col gap-4 md:flex-row md:items-start md:justify-between"><div class="min-w-0"><div class="flex flex-wrap items-center gap-2"><h3 class="font-semibold text-accent-foreground">{{ label(session) }}</h3><span class="rounded-full bg-accent px-2 py-1 text-xs font-semibold text-primary">{{ statusLabel(session) }}</span></div><dl class="mt-3 grid gap-2 text-sm text-muted-foreground sm:grid-cols-2"><div><dt class="font-semibold">Inicio</dt><dd>{{ session.createdAt | date:'medium' }}</dd></div><div><dt class="font-semibold">Última actividad</dt><dd>{{ session.lastUsedAt ? (session.lastUsedAt | date:'medium') : 'Sin información' }}</dd></div><div><dt class="font-semibold">Expira</dt><dd>{{ session.expiresAt | date:'medium' }}</dd></div><div><dt class="font-semibold">Navegador</dt><dd class="truncate" [title]="session.userAgent || ''">{{ session.userAgent || 'No disponible' }}</dd></div><div><dt class="font-semibold">IP</dt><dd>{{ session.ipAddress || 'No disponible' }}</dd></div></dl></div><button *ngIf="canRevoke(session)" type="button" (click)="revokeRequested.emit(session.id)" [disabled]="pendingAction !== null" class="rounded-full border border-destructive/20 px-4 py-2 text-sm font-semibold text-destructive">{{ session.current ? 'Cerrar esta sesión' : 'Cerrar sesión' }}</button></div></article>
      }
    </div>`
})
export class SessionListComponent {
  @Input({ required: true }) scope!: SessionScope;
  @Input() sessions: AuthSessionResponse[] = [];
  @Input() status: SessionLoadStatus = 'idle';
  @Input() error: string | null = null;
  @Input() pendingAction: SessionMutationKind | null = null;
  @Input() pendingSessionId: string | null = null;
  @Input() mutationError: string | null = null;
  @Output() refreshRequested = new EventEmitter<void>();
  @Output() revokeRequested = new EventEmitter<string>();
  @Output() revokeOthersRequested = new EventEmitter<void>();
  get canRevokeOthers(): boolean { return this.sessions.filter(session => this.active(session)).some(session => session.current) && this.sessions.filter(session => this.active(session)).length > 1; }
  canRevoke(session: AuthSessionResponse): boolean { return !session.revoked && this.state(session) !== 'EXPIRED'; }
  state(session: AuthSessionResponse): SessionDisplayStatus { return deriveSessionStatus(session, Date.now()); }
  active(session: AuthSessionResponse): boolean { const state = this.state(session); return state === 'CURRENT' || state === 'ACTIVE'; }
  label(session: AuthSessionResponse): string { return session.current ? 'Sesión identificada por el servidor' : 'Sesión adicional'; }
  statusLabel(session: AuthSessionResponse): string { return ({ CURRENT: 'Esta sesión', ACTIVE: 'Activa', EXPIRED: 'Expirada', REVOKED: 'Revocada' } as Record<SessionDisplayStatus, string>)[this.state(session)]; }
}

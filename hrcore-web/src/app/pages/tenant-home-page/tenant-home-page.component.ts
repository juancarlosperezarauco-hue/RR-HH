import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { RouterModule } from '@angular/router';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { TenantNavigationService } from '../../shared/lib/navigation/tenant-navigation.service';

@Component({
  selector: 'app-tenant-home-page', standalone: true, imports: [CommonModule, RouterModule],
  template: `
    <div class="space-y-6">
      @if (auth.status() === 'loading' || auth.status() === 'idle') {
        <section class="rounded-3xl border border-border bg-card p-8 text-sm text-muted-foreground">Verificando tu sesión...</section>
      } @else if (auth.currentUser(); as user) {
        <section class="rounded-3xl border border-border bg-card p-6 shadow-sm">
          <p class="text-sm font-semibold text-primary">Inicio</p>
          <h1 class="mt-2 text-3xl font-black text-accent-foreground">Hola, {{ user.firstName }}</h1>
          <p class="mt-2 text-sm text-muted-foreground">{{ user.email }} · Organización: {{ user.tenantSlug }}</p>
          <p class="mt-3 text-sm text-muted-foreground">{{ permissions.isCurrentOwner() ? 'Propietario de la organización' : 'Colaborador de la organización' }}</p>
        </section>
        <section class="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          @for (item of navigation.visibleItems(); track item.id) {
            @if (item.id !== 'home') {
              <a [routerLink]="item.route" class="rounded-2xl border border-border bg-card p-5 transition hover:border-primary hover:bg-accent">
                <p class="font-semibold text-accent-foreground">{{ item.label }}</p><p class="mt-1 text-xs text-muted-foreground">Abrir sección</p>
              </a>
            }
          }
          <a routerLink="/dashboard/profile" class="rounded-2xl border border-border bg-card p-5 transition hover:border-primary hover:bg-accent"><p class="font-semibold text-accent-foreground">Mi perfil</p><p class="mt-1 text-xs text-muted-foreground">Revisar tus datos</p></a>
        </section>
        @if (navigation.visibleItems().length <= 3) { <p class="rounded-2xl border border-border bg-muted p-4 text-sm text-muted-foreground">Tu cuenta está activa. Aún no tienes módulos administrativos asignados.</p> }
      } @else {
        <section class="rounded-3xl border border-border bg-card p-8 text-sm text-muted-foreground">No pudimos verificar tu sesión. Intenta nuevamente desde el inicio de sesión.</section>
      }
    </div>`
})
export class TenantHomePageComponent {
  readonly auth = inject(AuthFacade);
  readonly permissions = inject(PermissionService);
  readonly navigation = inject(TenantNavigationService);
}

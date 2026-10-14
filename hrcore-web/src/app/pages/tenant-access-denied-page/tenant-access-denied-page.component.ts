import { Component } from '@angular/core';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-tenant-access-denied-page', standalone: true, imports: [RouterModule],
  template: `<div class="mx-auto max-w-xl rounded-3xl border border-border bg-card p-8 text-center shadow-sm"><h1 class="text-2xl font-black text-accent-foreground">Acceso no disponible</h1><p class="mt-3 text-sm text-muted-foreground">No tienes permisos para acceder a esta sección.</p><div class="mt-6 flex justify-center gap-3"><a routerLink="/dashboard/home" class="rounded-full bg-primary px-4 py-2 text-sm font-semibold text-primary-foreground">Ir al inicio</a><a routerLink="/dashboard/profile" class="rounded-full border border-border px-4 py-2 text-sm font-semibold text-primary">Ver mi perfil</a></div></div>`
})
export class TenantAccessDeniedPageComponent {}

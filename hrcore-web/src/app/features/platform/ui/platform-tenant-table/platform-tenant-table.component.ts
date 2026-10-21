import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { PlatformTenant } from '../../../../entities/platform/api/platform.service';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-platform-tenant-table',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <div class="overflow-x-auto">
      @if (isLoading) {
        <div class="space-y-3">
          @for (i of [1,2,3,4,5]; track i) {
            <div class="bg-card rounded-xl border border-border p-4 animate-pulse">
              <div class="flex justify-between">
                <div class="space-y-2">
                  <div class="h-5 w-40 bg-muted rounded"></div>
                  <div class="h-4 w-28 bg-muted rounded"></div>
                </div>
                <div class="h-6 w-20 bg-muted rounded-full"></div>
              </div>
            </div>
          }
        </div>
      } @else {
        <table class="min-w-full divide-y divide-border text-left">
          <thead class="bg-muted/60 text-xs font-semibold uppercase tracking-wider text-muted-foreground">
            <tr>
              <th class="px-6 py-3.5">Organización</th>
              <th class="px-6 py-3.5">NIT / Identificación</th>
              <th class="px-6 py-3.5">Plan Contratado</th>
              <th class="px-6 py-3.5">Estado Operativo</th>
              <th class="px-6 py-3.5">Fecha Registro</th>
              <th class="px-6 py-3.5 text-right">Acciones</th>
            </tr>
          </thead>
          <tbody class="divide-y divide-border bg-card">
            @for (tenant of tenants; track tenant.id) {
              <tr class="transition-colors hover:bg-muted/40">
                <!-- Organización (Nombre comercial & Razón social) -->
                <td class="px-6 py-4">
                  <div class="flex items-center gap-3">
                    <div class="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary font-bold text-sm">
                      {{ (tenant.name || 'O').charAt(0).toUpperCase() }}
                    </div>
                    <div>
                      <div class="text-sm font-bold text-foreground">
                        {{ tenant.name }}
                      </div>
                      <div class="text-xs text-muted-foreground">
                        @if (tenant.legalName) {
                          <span class="font-medium text-foreground/80">{{ tenant.legalName }}</span> · 
                        }
                        <span>slug: {{ tenant.slug }}</span>
                      </div>
                    </div>
                  </div>
                </td>

                <!-- NIT / Identificación fiscal -->
                <td class="px-6 py-4 whitespace-nowrap">
                  @if (tenant.taxIdentifier) {
                    <span class="inline-flex items-center gap-1.5 rounded-lg border border-border bg-muted/60 px-2.5 py-1 text-xs font-mono font-medium text-foreground">
                      <lucide-icon name="file-text" class="h-3 w-3 text-muted-foreground"></lucide-icon>
                      {{ tenant.taxIdentifier }}
                    </span>
                  } @else {
                    <span class="text-xs italic text-muted-foreground">Sin NIT registrado</span>
                  }
                </td>

                <!-- Plan y Suscripción -->
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="flex flex-col gap-0.5">
                    <span class="text-sm font-semibold text-foreground">
                      {{ resolvePlanName(tenant) }}
                    </span>
                    @if (tenant.subscriptionStatus) {
                      <span class="text-[11px] font-medium uppercase tracking-wider text-muted-foreground">
                        Suscripción: {{ tenant.subscriptionStatus }}
                      </span>
                    }
                  </div>
                </td>

                <!-- Estado Operativo -->
                <td class="px-6 py-4 whitespace-nowrap">
                  @if (tenant.active) {
                    <span class="inline-flex items-center gap-1.5 rounded-full border border-emerald-500/20 bg-emerald-500/10 px-2.5 py-1 text-xs font-semibold text-emerald-600 dark:text-emerald-400">
                      <span class="h-1.5 w-1.5 rounded-full bg-emerald-500 animate-pulse"></span>
                      Activo
                    </span>
                  } @else {
                    <span class="inline-flex items-center gap-1.5 rounded-full border border-destructive/20 bg-destructive/10 px-2.5 py-1 text-xs font-semibold text-destructive">
                      <span class="h-1.5 w-1.5 rounded-full bg-destructive"></span>
                      Desactivado
                    </span>
                  }
                </td>

                <!-- Fecha Registro -->
                <td class="px-6 py-4 whitespace-nowrap text-sm text-muted-foreground">
                  {{ tenant.createdAt | date:'mediumDate' }}
                </td>

                <!-- Acciones -->
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm">
                  <div class="flex items-center justify-end gap-1.5">
                    <!-- Botón Ver detalles -->
                    <button
                      type="button"
                      (click)="viewDetails.emit(tenant.id)"
                      class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border text-foreground transition-colors hover:bg-accent hover:text-primary cursor-pointer"
                      title="Ver detalles de la organización">
                      <lucide-icon name="eye" class="h-4 w-4"></lucide-icon>
                    </button>

                    <!-- Botón Desactivar (si activo) -->
                    @if (tenant.active) {
                      <button
                        type="button"
                        (click)="deactivate.emit(tenant.id)"
                        class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border text-destructive transition-colors hover:bg-destructive/10 cursor-pointer"
                        title="Desactivar / suspender estado operativo">
                        <lucide-icon name="pause-circle" class="h-4 w-4"></lucide-icon>
                      </button>
                    }

                    <!-- Botón Activar (si inactivo) -->
                    @if (!tenant.active) {
                      <button
                        type="button"
                        (click)="activate.emit(tenant.id)"
                        class="inline-flex h-8 w-8 items-center justify-center rounded-lg border border-border text-emerald-600 transition-colors hover:bg-emerald-500/10 cursor-pointer"
                        title="Activar estado operativo">
                        <lucide-icon name="play-circle" class="h-4 w-4"></lucide-icon>
                      </button>
                    }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="6" class="px-6 py-12 text-center text-muted-foreground">
                  <div class="flex flex-col items-center justify-center gap-2">
                    <lucide-icon name="building-2" class="h-8 w-8 text-muted-foreground/60"></lucide-icon>
                    <p class="font-medium">No se encontraron organizaciones registradas</p>
                    <p class="text-xs">Intenta ajustar los filtros de búsqueda</p>
                  </div>
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `
})
export class PlatformTenantTableComponent {
  @Input() tenants: PlatformTenant[] = [];
  @Input() isLoading = false;
  @Input() planNames: Map<string, string> = new Map();

  @Output() viewDetails = new EventEmitter<string>();
  @Output() activate = new EventEmitter<string>();
  @Output() deactivate = new EventEmitter<string>();

  resolvePlanName(tenant: PlatformTenant): string {
    if (tenant.planName) {
      return tenant.planName;
    }
    if (tenant.planCode) {
      return tenant.planCode;
    }
    if (tenant.planId && this.planNames.has(tenant.planId)) {
      return this.planNames.get(tenant.planId)!;
    }
    return tenant.planId ? 'Plan asignado' : 'Sin plan';
  }
}

// features/platform/ui/platform-tenant-table/platform-tenant-table.component.ts
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
        <!-- Estado de carga (skeleton) -->
        <div class="space-y-3">
          @for (i of [1,2,3,4,5]; track i) {
            <div class="bg-white rounded-xl border border-[#C8E6C9] p-4 animate-pulse">
              <div class="flex justify-between">
                <div class="space-y-2">
                  <div class="h-5 w-32 bg-gray-200 rounded"></div>
                  <div class="h-4 w-24 bg-gray-200 rounded"></div>
                </div>
                <div class="h-6 w-16 bg-gray-200 rounded-full"></div>
              </div>
            </div>
          }
        </div>
      } @else {
        <table class="min-w-full divide-y divide-[#C8E6C9]">
          <thead class="bg-[#F1F8E9]">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Nombre</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Slug</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Estado</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Creado</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Acciones</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-[#C8E6C9]">
            @for (tenant of tenants; track tenant.id) {
              <tr class="hover:bg-[#F1F8E9] transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-[#2E7D32]">{{ tenant.name }}</div>
                  <div class="text-xs text-[#666666]">Schema: {{ tenant.schemaName }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full bg-[#E8F5E9] text-[#2E7D32]">
                    {{ tenant.slug }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <span [class]="tenant.active ? 'bg-[#E8F5E9] text-[#2E7D32]' : 'bg-[#FFEBEE] text-[#C62828]'" 
                        class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                    {{ tenant.status }}
                  </span>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-sm text-[#666666]">
                  {{ tenant.createdAt | date:'shortDate' }}
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <div class="flex justify-end gap-2">
                    <!-- ✅ Botón Ver detalles -->
                    <button (click)="viewDetails.emit(tenant.id)" 
                            class="cursor-pointer text-[#4CAF50] hover:text-[#2E7D32] transition-colors"
                            title="Ver detalles">
                      <lucide-icon name="eye" class="h-5 w-5"></lucide-icon>
                    </button>
                    
                    <!-- ✅ Botón Desactivar (solo si está activo) -->
                    @if (tenant.active) {
                      <button (click)="deactivate.emit(tenant.id)" 
                              class="cursor-pointer text-[#FF9800] hover:text-[#F57C00] transition-colors"
                              title="Desactivar">
                        <lucide-icon name="pause-circle" class="h-5 w-5"></lucide-icon>
                      </button>
                    }
                    
                    <!-- ✅ Botón Activar (solo si está inactivo) -->
                    @if (!tenant.active) {
                      <button (click)="activate.emit(tenant.id)" 
                              class="cursor-pointer text-[#4CAF50] hover:text-[#2E7D32] transition-colors"
                              title="Activar">
                        <lucide-icon name="play-circle" class="h-5 w-5"></lucide-icon>
                      </button>
                    }
                  </div>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="px-6 py-10 text-center text-[#666666]">
                  No hay tenants registrados
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
  @Output() viewDetails = new EventEmitter<string>();
  @Output() activate = new EventEmitter<string>();
  @Output() deactivate = new EventEmitter<string>();
}

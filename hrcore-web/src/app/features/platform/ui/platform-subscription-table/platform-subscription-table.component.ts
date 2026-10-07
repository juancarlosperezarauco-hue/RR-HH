// features/platform/ui/platform-subscription-table/platform-subscription-table.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule, Eye, Calendar, Clock } from 'lucide-angular';
import { PlatformSubscription } from '../../../../entities/platform/api/platform.service';

@Component({
  selector: 'app-platform-subscription-table',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <div class="overflow-x-auto">
      @if (isLoading) {
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
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Tenant</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Plan</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Estado</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Expira</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-[#2E7D32] uppercase tracking-wider">Acciones</th>
            </tr>
          </thead>
          <tbody class="bg-white divide-y divide-[#C8E6C9]">
            @for (sub of subscriptions; track sub.id) {
              <tr class="hover:bg-[#F1F8E9] transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-[#2E7D32]">{{ sub.tenantName }}</div>
                  <div class="text-xs text-[#666666]">Slug: {{ sub.tenantSlug }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div>
                    <span class="font-medium text-[#2E7D32]">{{ sub.planName }}</span>
                    <div class="text-xs text-[#666666]">{{ sub.planCode }} - {{ sub.planType }}</div>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  @if (sub.trial) {
                    <span class="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-[#E8F5E9] text-[#2E7D32]">
                      <lucide-icon name="clock" class="h-3 w-3"></lucide-icon>
                      Prueba
                    </span>
                  } @else {
                    <span [class]="sub.status === 'ACTIVE' ? 'bg-[#E8F5E9] text-[#2E7D32]' : 'bg-[#FFEBEE] text-[#C62828]'" 
                          class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                      {{ sub.status }}
                    </span>
                  }
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-[#666666]">
                    {{ sub.expiresAt | date:'shortDate' }}
                  </div>
                  <div class="text-xs text-[#FF9800]" *ngIf="sub.remainingDays <= 7 && sub.remainingDays > 0">
                    ⚠️ {{ sub.remainingDays }} días restantes
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button (click)="viewDetails.emit(sub.id)" 
                          class="cursor-pointer text-[#4CAF50] hover:text-[#2E7D32] transition-colors"
                          title="Ver detalles">
                    <lucide-icon name="eye" class="h-5 w-5"></lucide-icon>
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="px-6 py-10 text-center text-[#666666]">
                  No hay suscripciones registradas
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `
})
export class PlatformSubscriptionTableComponent {
  @Input() subscriptions: PlatformSubscription[] = [];
  @Input() isLoading = false;
  @Output() viewDetails = new EventEmitter<string>();
}

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
            <div class="bg-card rounded-xl border border-border p-4 animate-pulse">
              <div class="flex justify-between">
                <div class="space-y-2">
                  <div class="h-5 w-32 bg-muted rounded"></div>
                  <div class="h-4 w-24 bg-muted rounded"></div>
                </div>
                <div class="h-6 w-16 bg-muted rounded-full"></div>
              </div>
            </div>
          }
        </div>
      } @else {
        <table class="min-w-full divide-y divide-border">
          <thead class="bg-accent">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Tenant</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Plan</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Estado</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Expira</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-primary uppercase tracking-wider">Acciones</th>
            </tr>
          </thead>
          <tbody class="bg-card divide-y divide-border">
            @for (sub of subscriptions; track sub.id) {
              <tr class="hover:bg-accent transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-primary">{{ sub.tenantName }}</div>
                  <div class="text-xs text-muted-foreground">Slug: {{ sub.tenantSlug }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div>
                    <span class="font-medium text-primary">{{ sub.planName }}</span>
                    <div class="text-xs text-muted-foreground">{{ sub.planCode }} - {{ sub.planType }}</div>
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  @if (sub.trial) {
                    <span class="inline-flex items-center gap-1 px-2 py-1 text-xs font-semibold rounded-full bg-accent text-primary">
                      <lucide-icon name="clock" class="h-3 w-3"></lucide-icon>
                      Prueba
                    </span>
                  } @else {
                    <span [class]="sub.status === 'ACTIVE' ? 'bg-accent text-primary' : 'bg-muted text-destructive'"
                          class="px-2 inline-flex text-xs leading-5 font-semibold rounded-full">
                      {{ sub.status }}
                    </span>
                  }
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm text-muted-foreground">
                    {{ sub.expiresAt | date:'shortDate' }}
                  </div>
                  <div class="text-xs text-foreground" *ngIf="sub.remainingDays <= 7 && sub.remainingDays > 0">
                    ⚠️ {{ sub.remainingDays }} días restantes
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button (click)="viewDetails.emit(sub.id)" 
                          class="cursor-pointer text-secondary hover:text-primary transition-colors"
                          title="Ver detalles">
                    <lucide-icon name="eye" class="h-5 w-5"></lucide-icon>
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="px-6 py-10 text-center text-muted-foreground">
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

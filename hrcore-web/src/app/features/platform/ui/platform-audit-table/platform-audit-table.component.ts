// features/platform/ui/platform-audit-table/platform-audit-table.component.ts
import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { AuditEvent } from '../../../../entities/platform/api/platform.service';

@Component({
  selector: 'app-platform-audit-table',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="overflow-x-auto">
      @if (isLoading) {
        <div class="space-y-3">
          @for (i of [1,2,3,4,5]; track i) {
            <div class="bg-card rounded-xl border border-border p-4 animate-pulse">
              <div class="flex gap-4">
                <div class="h-5 w-24 bg-muted rounded"></div>
                <div class="h-5 w-32 bg-muted rounded"></div>
              </div>
            </div>
          }
        </div>
      } @else {
        <table class="min-w-full divide-y divide-border">
          <thead class="bg-accent">
            <tr>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Fecha</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Actor</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Evento</th>
              <th class="px-6 py-3 text-left text-xs font-medium text-primary uppercase tracking-wider">Origen</th>
              <th class="px-6 py-3 text-right text-xs font-medium text-primary uppercase tracking-wider">Acciones</th>
            </tr>
          </thead>
          <tbody class="bg-card divide-y divide-border">
            @for (event of events; track event.id) {
              <tr class="hover:bg-accent transition-colors">
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-primary">{{ event.createdAt | date:'medium' }}</div>
                  <div
                    class="mt-1 inline-flex items-center rounded-full px-2 py-1 text-xs font-semibold"
                    [class.bg-accent]="(event.outcome || 'SUCCESS') === 'SUCCESS'"
                    [class.text-primary]="(event.outcome || 'SUCCESS') === 'SUCCESS'"
                    [class.bg-muted]="(event.outcome || 'SUCCESS') !== 'SUCCESS'"
                    [class.text-destructive]="(event.outcome || 'SUCCESS') !== 'SUCCESS'">
                    {{ event.outcome || 'SUCCESS' }}
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-primary">{{ event.actorSubject }}</div>
                  <div class="text-xs text-muted-foreground">{{ event.actorEmail || 'Sin correo' }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-primary">{{ event.eventType }}</div>
                  <div class="text-xs text-muted-foreground">{{ event.resourceType }}</div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap">
                  <div class="text-sm font-medium text-primary">{{ event.ipAddress || 'N/A' }}</div>
                  <div class="text-xs text-muted-foreground">
                    {{ event.source || event.userAgent || 'Sin origen' }}
                  </div>
                </td>
                <td class="px-6 py-4 whitespace-nowrap text-right text-sm font-medium">
                  <button
                    type="button"
                    (click)="viewDetails.emit(event)"
                    class="cursor-pointer text-secondary transition-colors hover:text-primary"
                    title="Ver detalle">
                    <lucide-icon name="eye" class="h-5 w-5"></lucide-icon>
                  </button>
                </td>
              </tr>
            } @empty {
              <tr>
                <td colspan="5" class="px-6 py-10 text-center text-muted-foreground">
                  No hay eventos de auditoría
                </td>
              </tr>
            }
          </tbody>
        </table>
      }
    </div>
  `
})
export class PlatformAuditTableComponent {
  @Input() events: AuditEvent[] = [];
  @Input() isLoading = false;
  @Output() viewDetails = new EventEmitter<AuditEvent>();
}

import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { EmployeeDocumentAuditResponse } from '../../../../entities/employee-file';

@Component({
  selector: 'app-document-timeline',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="rounded-2xl border border-border bg-card p-6 shadow-sm space-y-6">
      <div class="flex items-center justify-between border-b border-border pb-4">
        <div>
          <h3 class="text-base font-bold text-accent-foreground">Línea de Tiempo y Auditoría Inmutable</h3>
          <p class="text-xs text-muted-foreground mt-0.5">Trazabilidad legal de visualizaciones, descargas y modificaciones en el expediente.</p>
        </div>
        <span class="inline-flex items-center gap-1.5 rounded-full bg-secondary/10 px-3 py-1 text-xs font-semibold text-secondary-foreground border border-secondary/20">
          <lucide-icon name="shield-check" [size]="14"></lucide-icon>
          Auditoría Verificada
        </span>
      </div>

      <!-- Timeline vertical -->
      <div class="relative pl-6 space-y-8 before:absolute before:left-2.5 before:top-3 before:bottom-3 before:w-0.5 before:bg-border">
        <div *ngFor="let audit of audits" class="relative group">
          <!-- Icono de Nodo -->
          <div
            [class]="getNodeClass(audit.action)"
            class="absolute -left-6 top-0 flex h-6 w-6 items-center justify-center rounded-full border border-card shadow-sm">
            <lucide-icon [name]="getActionIcon(audit.action)" [size]="12"></lucide-icon>
          </div>

          <!-- Contenido del Evento -->
          <div class="rounded-xl border border-border/80 bg-muted/20 p-4 transition-colors hover:bg-muted/40">
            <div class="flex items-center justify-between gap-2">
              <span class="text-xs font-bold uppercase tracking-wider text-accent-foreground">
                {{ formatAction(audit.action) }}
              </span>
              <span class="text-[11px] font-medium text-muted-foreground">
                {{ formatTime(audit.performedAt) }}
              </span>
            </div>

            <p class="text-xs text-muted-foreground mt-1">
              Ejecutado por <span class="font-semibold text-foreground">{{ audit.userEmail || 'Responsable de RRHH' }}</span>
              <span *ngIf="audit.ipAddress"> desde IP: {{ audit.ipAddress }}</span>
            </p>
          </div>
        </div>
      </div>
    </div>
  `
})
export class DocumentTimelineComponent {
  @Input() audits: EmployeeDocumentAuditResponse[] = [];

  getActionIcon(action: string): string {
    switch (action) {
      case 'UPLOAD': return 'upload';
      case 'VIEW': return 'eye';
      case 'DOWNLOAD': return 'download';
      case 'ARCHIVE': return 'trash-2';
      default: return 'file-text';
    }
  }

  getNodeClass(action: string): string {
    switch (action) {
      case 'UPLOAD': return 'bg-emerald-500 text-white';
      case 'VIEW': return 'bg-blue-500 text-white';
      case 'DOWNLOAD': return 'bg-purple-500 text-white';
      case 'ARCHIVE': return 'bg-rose-500 text-white';
      default: return 'bg-primary text-primary-foreground';
    }
  }

  formatAction(action: string): string {
    switch (action) {
      case 'UPLOAD': return 'Carga de Documento Digital';
      case 'VIEW': return 'Visualización Embebida (URL Firmada)';
      case 'DOWNLOAD': return 'Descarga Segura de Archivo';
      case 'ARCHIVE': return 'Baja Lógica / Archivado Legal';
      default: return action;
    }
  }

  formatTime(isoString: string): string {
    if (!isoString) return '';
    const date = new Date(isoString);
    return date.toLocaleString('es-BO', {
      day: '2-digit',
      month: 'short',
      year: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    });
  }
}

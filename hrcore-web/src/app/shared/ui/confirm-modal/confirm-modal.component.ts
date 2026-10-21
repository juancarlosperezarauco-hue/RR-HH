import { Component, HostListener, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule, AlertTriangle, AlertCircle, CheckCircle, Info, X } from 'lucide-angular';
import { ConfirmModalService } from './confirm-modal.service';

@Component({
  selector: 'app-confirm-modal',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    @if (service.isOpen()) {
      <div
        class="fixed inset-0 z-[100] flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm transition-opacity"
        (click)="onBackdropClick($event)">
        
        <div
          class="relative w-full max-w-md overflow-hidden rounded-[28px] border border-border bg-card p-6 shadow-2xl transition-all sm:p-7"
          (click)="$event.stopPropagation()">
          
          <!-- Botón de Cerrar (X) -->
          <button
            type="button"
            (click)="service.handleCancel()"
            class="absolute right-5 top-5 inline-flex h-8 w-8 items-center justify-center rounded-full text-muted-foreground transition-colors hover:bg-muted hover:text-foreground">
            <lucide-icon name="x" [size]="18"></lucide-icon>
          </button>

          <!-- Encabezado con Icono -->
          <div class="flex items-start gap-4">
            <div
              class="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl border"
              [ngClass]="{
                'border-destructive/30 bg-destructive/10 text-destructive': service.options().variant === 'danger',
                'border-amber-500/30 bg-amber-500/10 text-amber-600': service.options().variant === 'warning',
                'border-emerald-500/30 bg-emerald-500/10 text-emerald-600': service.options().variant === 'success',
                'border-primary/30 bg-primary/10 text-primary': service.options().variant === 'info'
              }">
              @switch (service.options().variant) {
                @case ('danger') { <lucide-icon name="alert-triangle" [size]="24"></lucide-icon> }
                @case ('warning') { <lucide-icon name="alert-circle" [size]="24"></lucide-icon> }
                @case ('success') { <lucide-icon name="check-circle" [size]="24"></lucide-icon> }
                @default { <lucide-icon name="info" [size]="24"></lucide-icon> }
              }
            </div>

            <div class="min-w-0 flex-1 pt-0.5">
              <h3 class="text-lg font-bold text-accent-foreground sm:text-xl">
                {{ service.options().title }}
              </h3>
              <p class="mt-2 text-sm leading-relaxed text-foreground/80">
                {{ service.options().message }}
              </p>
            </div>
          </div>

          <!-- Caja de detalles / advertencia técnica adicional (opcional) -->
          @if (service.options().details) {
            <div class="mt-4 rounded-2xl border border-border bg-muted/40 p-3.5 text-xs leading-relaxed text-muted-foreground">
              {{ service.options().details }}
            </div>
          }

          <!-- Botones de Acción -->
          <div class="mt-6 flex flex-col-reverse gap-2.5 sm:flex-row sm:justify-end">
            <button
              type="button"
              (click)="service.handleCancel()"
              class="inline-flex h-11 items-center justify-center rounded-full border border-border bg-card px-5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-accent hover:text-foreground">
              {{ service.options().cancelText }}
            </button>

            <button
              type="button"
              (click)="service.handleConfirm()"
              class="inline-flex h-11 items-center justify-center rounded-full px-6 text-sm font-bold shadow-sm transition-all focus:outline-none focus:ring-2 focus:ring-offset-2"
              [ngClass]="{
                'bg-destructive text-destructive-foreground hover:bg-destructive/90 focus:ring-destructive': service.options().variant === 'danger',
                'bg-amber-600 text-white hover:bg-amber-700 focus:ring-amber-600': service.options().variant === 'warning',
                'bg-emerald-600 text-white hover:bg-emerald-700 focus:ring-emerald-600': service.options().variant === 'success',
                'bg-primary text-primary-foreground hover:bg-primary/90 focus:ring-primary': service.options().variant === 'info'
              }">
              {{ service.options().confirmText }}
            </button>
          </div>
        </div>
      </div>
    }
  `
})
export class ConfirmModalComponent {
  readonly service = inject(ConfirmModalService);

  @HostListener('document:keydown.escape')
  onEscape(): void {
    if (this.service.isOpen()) {
      this.service.handleCancel();
    }
  }

  onBackdropClick(event: MouseEvent): void {
    if (event.target === event.currentTarget) {
      this.service.handleCancel();
    }
  }
}

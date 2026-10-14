import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { LeaveBalanceResponse } from '../../../../entities/absence';

@Component({
  selector: 'app-vacation-balance-card',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div
      class="relative overflow-hidden rounded-2xl border border-border bg-gradient-to-br from-card via-card to-accent/25 p-6 shadow-sm transition-all"
    >
      <!-- Background Ambient Glow -->
      <div
        class="pointer-events-none absolute -right-12 -top-12 h-44 w-44 rounded-full bg-emerald-500/10 blur-3xl"
      ></div>

      <div class="relative z-10 flex flex-col md:flex-row md:items-center md:justify-between gap-6">
        
        <!-- Left: Saldo Remanente Principal -->
        <div class="space-y-2">
          <div class="flex items-center gap-2">
            <span class="inline-flex items-center gap-1.5 rounded-md bg-emerald-500/10 px-2.5 py-0.5 text-xs font-semibold text-emerald-700 dark:text-emerald-400">
              <lucide-icon name="check-circle" class="h-3.5 w-3.5"></lucide-icon>
              Saldo de Vacaciones LGT
            </span>

            <!-- Badge de Antigüedad -->
            <span
              *ngIf="balance"
              class="inline-flex items-center gap-1 rounded-md bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary border border-primary/20"
            >
              {{ balance.yearsOfService ?? 1 }} años de antigüedad (Escala {{ balance.entitledDays }}d)
            </span>
          </div>

          <div class="flex items-baseline gap-2">
            <span class="text-4xl sm:text-5xl font-black tracking-tight text-foreground">
              {{ balance?.remainingDays ?? 0 }}
            </span>
            <span class="text-base font-semibold text-muted-foreground">
              días hábiles disponibles
            </span>
          </div>

          <!-- Barra de Progreso de Consumo -->
          <div class="w-full max-w-md pt-1">
            <div class="flex items-center justify-between text-xs text-muted-foreground mb-1">
              <span>Consumo del período</span>
              <span class="font-mono font-bold">{{ usagePercentage }}% utilizado</span>
            </div>
            <div class="h-2 w-full overflow-hidden rounded-full bg-muted">
              <div
                class="h-full rounded-full bg-gradient-to-r from-emerald-500 to-teal-500 transition-all duration-500"
                [style.width.%]="usagePercentage"
              ></div>
            </div>
          </div>
        </div>

        <!-- Center: Mini desglose de días -->
        <div class="grid grid-cols-3 gap-3 sm:gap-4 py-2 border-y md:border-y-0 md:border-x border-border/60 md:px-6">
          <div class="text-center">
            <p class="text-xs font-medium text-muted-foreground">Derecho Anual</p>
            <p class="mt-1 text-lg font-bold text-foreground">{{ balance?.entitledDays ?? 0 }}d</p>
            <span class="text-[10px] text-muted-foreground">Ley Boliviana</span>
          </div>
          <div class="text-center">
            <p class="text-xs font-medium text-muted-foreground">Gozados</p>
            <p class="mt-1 text-lg font-bold text-emerald-600 dark:text-emerald-400">{{ balance?.usedDays ?? 0 }}d</p>
            <span class="text-[10px] text-muted-foreground">Tomados</span>
          </div>
          <div class="text-center">
            <p class="text-xs font-medium text-muted-foreground">En Trámite</p>
            <p class="mt-1 text-lg font-bold text-amber-600 dark:text-amber-400">{{ balance?.pendingDays ?? 0 }}d</p>
            <span class="text-[10px] text-muted-foreground">Pendientes</span>
          </div>
        </div>

        <!-- Right: Botón de Nueva Solicitud -->
        <div class="flex flex-col items-stretch md:items-end justify-center min-w-[180px]">
          <button
            type="button"
            (click)="requestLeave.emit()"
            class="inline-flex items-center justify-center gap-2 rounded-xl bg-primary px-5 py-3 text-sm font-bold text-primary-foreground shadow-md hover:bg-primary/90 transition-all transform active:scale-95"
          >
            <lucide-icon name="plus" class="h-4 w-4"></lucide-icon>
            Solicitar Ausencia
          </button>
          <span class="mt-2 text-[11px] text-muted-foreground text-center md:text-right">
            Cómputo automático de días hábiles
          </span>
        </div>

      </div>
    </div>
  `
})
export class VacationBalanceCardComponent {
  @Input() balance: LeaveBalanceResponse | null = null;
  @Output() requestLeave = new EventEmitter<void>();

  get usagePercentage(): number {
    if (!this.balance || this.balance.entitledDays === 0) return 0;
    const pct = (this.balance.usedDays / this.balance.entitledDays) * 100;
    return Math.min(100, Math.round(pct));
  }
}

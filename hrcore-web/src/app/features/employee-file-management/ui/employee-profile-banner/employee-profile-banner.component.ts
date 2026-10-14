import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

export interface EmployeeProfile {
  id: string;
  name: string;
  position: string;
  department: string;
  ci: string;
  contractType: string;
  branch: string;
  tenure: string;
  status: string;
}

@Component({
  selector: 'app-employee-profile-banner',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="rounded-2xl border border-border bg-card p-5 shadow-sm">
      <div class="flex flex-col gap-4 md:flex-row md:items-center md:justify-between">
        <div class="flex items-center gap-4">
          <div class="flex h-14 w-14 shrink-0 items-center justify-center rounded-2xl bg-primary/10 text-primary font-black text-lg border border-primary/20">
            {{ employee.name.substring(0, 2).toUpperCase() }}
          </div>
          <div>
            <div class="flex flex-wrap items-center gap-2">
              <h2 class="text-lg font-bold text-accent-foreground">{{ employee.name }}</h2>
              <span class="inline-flex items-center gap-1 rounded-full bg-emerald-500/10 px-2.5 py-0.5 text-[11px] font-bold text-emerald-600 border border-emerald-500/20">
                <span class="h-1.5 w-1.5 rounded-full bg-emerald-500"></span>
                {{ employee.status }}
              </span>
            </div>
            <p class="text-xs text-muted-foreground mt-0.5">
              {{ employee.position }} • {{ employee.department }} • {{ employee.branch }}
            </p>
            <div class="mt-2 flex flex-wrap gap-2 text-[11px] text-muted-foreground">
              <span class="rounded-md bg-muted/50 px-2 py-0.5 font-medium border border-border">CI: {{ employee.ci }}</span>
              <span class="rounded-md bg-muted/50 px-2 py-0.5 font-medium border border-border">Contrato: {{ employee.contractType }}</span>
              <span class="rounded-md bg-muted/50 px-2 py-0.5 font-medium border border-border">Antigüedad: {{ employee.tenure }}</span>
              <span class="rounded-md bg-muted/50 px-2 py-0.5 font-medium border border-border">Régimen LGT Bolivia</span>
            </div>
          </div>
        </div>

        <!-- Selector de Expediente -->
        <div class="min-w-[240px]">
          <label class="block text-[11px] font-bold uppercase tracking-wider text-muted-foreground mb-1.5">
            Expediente del Colaborador
          </label>
          <select
            [ngModel]="employee.id"
            (ngModelChange)="employeeChange.emit($event)"
            class="w-full rounded-xl border border-border bg-muted/40 px-3.5 py-2 text-xs font-semibold text-foreground outline-none focus:border-primary focus:ring-1 focus:ring-primary cursor-pointer">
            <option *ngFor="let emp of employees" [value]="emp.id">
              {{ emp.name }} ({{ emp.ci }})
            </option>
          </select>
        </div>
      </div>
    </section>
  `
})
export class EmployeeProfileBannerComponent {
  @Input({ required: true }) employee!: EmployeeProfile;
  @Input() employees: EmployeeProfile[] = [];
  @Output() employeeChange = new EventEmitter<string>();
}

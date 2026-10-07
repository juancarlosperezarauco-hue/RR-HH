// features/platform/ui/platform-subscription-assign-form/platform-subscription-assign-form.component.ts
import { Component, EventEmitter, Input, Output, inject, OnInit } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';
import { PlatformTenant } from '../../../../entities/platform/api/platform.service';
import { PlatformTenantListUseCase } from '../../application/platform-tenant-list.usecase';

@Component({
  selector: 'app-platform-subscription-assign-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    <div class="bg-white rounded-xl border border-[#C8E6C9] p-6">
      <h2 class="text-xl font-bold text-[#2E7D32] mb-4">Asignar/Modificar Suscripción</h2>
      
      <div *ngIf="error" class="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
        {{ error }}
      </div>
      
      <form [formGroup]="assignForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <div>
          <label class="block text-sm font-medium text-[#2E7D32] mb-1">Tenant</label>
          <select formControlName="tenantId"
                  class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <option value="">Selecciona un tenant...</option>
            @for (tenant of tenants; track tenant.id) {
              <option [value]="tenant.id">{{ tenant.name }} ({{ tenant.slug }})</option>
            }
          </select>
          <span *ngIf="isFieldInvalid('tenantId')" class="text-xs text-red-600 mt-1">Selecciona un tenant</span>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-[#2E7D32] mb-1">Plan</label>
          <select formControlName="planCode"
                  class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <option value="DEMO">DEMO - Prueba gratuita (10 días, 5 usuarios, 3 roles)</option>
            <option value="BASIC">BASIC - Básico</option>
            <option value="PRO">PRO - Profesional</option>
            <option value="ENTERPRISE">ENTERPRISE - Empresarial</option>
          </select>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-[#2E7D32] mb-1">Días de prueba (opcional)</label>
          <input type="number" formControlName="overrideTrialDays" 
                 placeholder="Dejar vacío para usar valor por defecto"
                 class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
          <p class="text-xs text-[#666666] mt-1">Solo aplica para planes DEMO</p>
        </div>
        
        <div class="flex justify-end gap-3 pt-4">
          <button type="button" (click)="cancel.emit()" 
                  class="px-4 py-2 border border-[#C8E6C9] text-[#666666] rounded-lg hover:bg-[#F1F8E9] transition-colors">
            Cancelar
          </button>
          <button type="submit" [disabled]="isLoading || assignForm.invalid" 
                  class="px-4 py-2 bg-[#2E7D32] text-white rounded-lg hover:bg-[#4CAF50] transition-colors disabled:opacity-50">
            {{ isLoading ? 'Asignando...' : 'Asignar suscripción' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class PlatformSubscriptionAssignFormComponent implements OnInit {
  @Input() isLoading = false;
  @Input() error: string | null = null;
  @Output() submitForm = new EventEmitter<{ tenantId: string; planCode: string; overrideTrialDays?: number }>();
  @Output() cancel = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  private tenantListUseCase = inject(PlatformTenantListUseCase);

  tenants: PlatformTenant[] = [];

  assignForm = this.fb.group({
    tenantId: ['', Validators.required],
    planCode: ['DEMO', Validators.required],
    overrideTrialDays: [null]
  });

  ngOnInit(): void {
    this.loadTenants();
  }

  async loadTenants(): Promise<void> {
    await this.tenantListUseCase.loadTenants(0, 1000);
    this.tenants = this.tenantListUseCase.tenants();
  }

  onSubmit(): void {
    if (this.assignForm.valid) {
      const value = this.assignForm.value;
      this.submitForm.emit({
        tenantId: value.tenantId!,
        planCode: value.planCode!,
        overrideTrialDays: value.overrideTrialDays || undefined
      });
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.assignForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
}

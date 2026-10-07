// features/platform/ui/platform-plan-form/platform-plan-form.component.ts
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-platform-plan-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    <div class="bg-white rounded-xl border border-[#C8E6C9] p-6">
      <h2 class="text-xl font-bold text-[#2E7D32] mb-4">Crear nuevo plan</h2>
      
      <div *ngIf="error" class="mb-4 p-3 bg-red-50 text-red-700 rounded-lg text-sm">
        {{ error }}
      </div>
      
      <form [formGroup]="planForm" (ngSubmit)="onSubmit()" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Código</label>
            <input type="text" formControlName="code" placeholder="Ej: PREMIUM"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('code')" class="text-xs text-red-600 mt-1">Código requerido</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Nombre</label>
            <input type="text" formControlName="name" placeholder="Ej: Premium"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('name')" class="text-xs text-red-600 mt-1">Nombre requerido</span>
          </div>
        </div>
        
        <div>
          <label class="block text-sm font-medium text-[#2E7D32] mb-1">Descripción</label>
          <textarea formControlName="description" rows="2" placeholder="Descripción del plan"
                    class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]"></textarea>
        </div>
        
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Tipo de plan</label>
            <select formControlName="planType"
                    class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
              <option value="PAID">PAID - Pago</option>
              <option value="DEMO">DEMO - Demostración</option>
            </select>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Días de prueba (solo DEMO)</label>
            <input type="number" formControlName="trialDays" placeholder="Ej: 10"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
          </div>
        </div>
        
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Máx. usuarios</label>
            <input type="number" formControlName="maxUsers" placeholder="Ej: 50"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('maxUsers')" class="text-xs text-red-600 mt-1">Valor requerido</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Máx. roles</label>
            <input type="number" formControlName="maxRoles" placeholder="Ej: 20"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('maxRoles')" class="text-xs text-red-600 mt-1">Valor requerido</span>
          </div>
        </div>
        
        <div class="flex justify-end gap-3 pt-4">
          <button type="button" (click)="cancel.emit()" 
                  class="cursor-pointer px-4 py-2 border border-[#C8E6C9] text-[#666666] rounded-lg hover:bg-[#F1F8E9] transition-colors">
            Cancelar
          </button>
          <button type="submit" [disabled]="isLoading || planForm.invalid" 
                  class="cursor-pointer px-4 py-2 bg-[#2E7D32] text-white rounded-lg hover:bg-[#4CAF50] transition-colors disabled:opacity-50">
            {{ isLoading ? 'Creando...' : 'Crear plan' }}
          </button>
        </div>
      </form>
    </div>
  `
})
export class PlatformPlanFormComponent {
  @Input() isLoading = false;
  @Input() error: string | null = null;
  @Output() submitForm = new EventEmitter<any>();
  @Output() cancel = new EventEmitter<void>();

  private fb = inject(FormBuilder);

  planForm = this.fb.group({
    code: ['', Validators.required],
    name: ['', Validators.required],
    description: [''],
    planType: ['PAID', Validators.required],
    maxUsers: [10, [Validators.required, Validators.min(1)]],
    maxRoles: [5, [Validators.required, Validators.min(1)]],
    trialDays: [null]
  });

  onSubmit(): void {
    if (this.planForm.valid) {
      this.submitForm.emit(this.planForm.value);
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.planForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
}

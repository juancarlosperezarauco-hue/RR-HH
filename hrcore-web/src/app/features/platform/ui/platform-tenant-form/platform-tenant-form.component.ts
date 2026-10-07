// features/platform/ui/platform-tenant-form/platform-tenant-form.component.ts
import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { LucideAngularModule } from 'lucide-angular';

export interface TenantPlanOption {
  id: string;
  code: string;
  name: string;
  description: string;
  maxUsers: number;
  maxRoles: number;
  planType: string;
  active: boolean;
  trialDays: number | null;
}

export interface CreateTenantFormData {
  name: string;
  slug: string;
  planCode: string;
  adminEmail: string;
  password: string;
  firstName: string;
  lastName: string;
}

@Component({
  selector: 'app-platform-tenant-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  template: `
    <!-- ✅ IMPORTANTE: El formGroup debe estar en el elemento que contiene los campos -->
    <form [formGroup]="tenantForm" (ngSubmit)="onSubmit()" class="space-y-6">
      <!-- Información de la empresa -->
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <div class="h-8 w-1 rounded-full bg-[#2E7D32]"></div>
          <h3 class="text-base font-semibold text-[#1B5E20]">Información de la empresa</h3>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Nombre de la empresa *</label>
            <input type="text" formControlName="name" placeholder="Ej: Mi Empresa SRL"
                   [class.border-red-500]="isFieldInvalid('name')"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('name')" class="text-xs text-red-600 mt-1">El nombre es obligatorio</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Slug (identificador único) *</label>
            <input type="text" formControlName="slug" placeholder="Ej: miempresa"
                   [class.border-red-500]="isFieldInvalid('slug')"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('slug')" class="text-xs text-red-600 mt-1">
              Solo letras minúsculas, números y guiones. Mínimo 3 caracteres.
            </span>
          </div>
        </div>

        <div>
          <label class="block text-sm font-medium text-[#2E7D32] mb-1">Plan *</label>
          <select formControlName="planCode"
                  [class.border-red-500]="isFieldInvalid('planCode')"
                  class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <option value="" disabled>Selecciona un plan...</option>
            @for (plan of plans; track plan.code) {
              <option [value]="plan.code">
                {{ plan.name }} ({{ plan.planType }}) - {{ plan.maxUsers }} usuarios
              </option>
            }
          </select>
          <span *ngIf="isFieldInvalid('planCode')" class="text-xs text-red-600 mt-1">Selecciona un plan</span>
        </div>
      </div>

      <!-- Administrador principal -->
      <div class="space-y-3">
        <div class="flex items-center gap-3">
          <div class="h-8 w-1 rounded-full bg-[#2E7D32]"></div>
          <h3 class="text-base font-semibold text-[#1B5E20]">Administrador principal</h3>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Nombre *</label>
            <input type="text" formControlName="firstName" placeholder="Ej: Juan"
                   [class.border-red-500]="isFieldInvalid('firstName')"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('firstName')" class="text-xs text-red-600 mt-1">El nombre es obligatorio</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Apellido *</label>
            <input type="text" formControlName="lastName" placeholder="Ej: Pérez"
                   [class.border-red-500]="isFieldInvalid('lastName')"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('lastName')" class="text-xs text-red-600 mt-1">El apellido es obligatorio</span>
          </div>
        </div>

        <div class="grid gap-4 sm:grid-cols-2">
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Correo electrónico *</label>
            <input type="email" formControlName="adminEmail" placeholder="admin@empresa.com"
                   [class.border-red-500]="isFieldInvalid('adminEmail')"
                   class="w-full px-3 py-2 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
            <span *ngIf="isFieldInvalid('adminEmail')" class="text-xs text-red-600 mt-1">Ingresa un correo válido</span>
          </div>
          <div>
            <label class="block text-sm font-medium text-[#2E7D32] mb-1">Contraseña *</label>
            <div class="relative">
              <input
                [type]="showPassword ? 'text' : 'password'"
                formControlName="password"
                placeholder="Mínimo 8 caracteres"
                [class.border-red-500]="isFieldInvalid('password')"
                class="w-full px-3 py-2 pr-10 border border-[#C8E6C9] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#4CAF50]">
              <button
                type="button"
                (click)="togglePasswordVisibility()"
                class="absolute inset-y-0 right-0 flex items-center pr-3 text-[#6B7D6C] transition-colors hover:text-[#2E7D32]"
                tabindex="-1">
                <lucide-icon [name]="showPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
              </button>
            </div>
            <span *ngIf="isFieldInvalid('password')" class="text-xs text-red-600 mt-1">La contraseña es requerida (mínimo 8 caracteres)</span>
          </div>
        </div>
      </div>

      <!-- Botones -->
      <div class="flex justify-end gap-3 pt-4 border-t border-[#C8E6C9]">
        <button type="button" (click)="cancel.emit()" 
                class="px-4 py-2 border border-[#C8E6C9] text-[#666666] rounded-lg hover:bg-[#F1F8E9] transition-colors">
          Cancelar
        </button>
        <button type="submit" 
                [disabled]="isLoading || tenantForm.invalid" 
                class="px-4 py-2 bg-[#2E7D32] text-white rounded-lg hover:bg-[#4CAF50] transition-colors disabled:opacity-50 disabled:bg-gray-400">
          {{ isLoading ? 'Creando...' : 'Crear tenant' }}
        </button>
      </div>
    </form>
  `
})
export class PlatformTenantFormComponent {
  @Input() isLoading = false;
  @Input() error: string | null = null;
  @Input() plans: TenantPlanOption[] = [];
  @Output() submitForm = new EventEmitter<CreateTenantFormData>();
  @Output() cancel = new EventEmitter<void>();

  private fb = inject(FormBuilder);
  showPassword = false;
  ngOnInit(): void {
    // Si hay planes disponibles y no hay valor seleccionado, selecciona el primero
    if (this.plans.length > 0 && !this.tenantForm.get('planCode')?.value) {
      this.tenantForm.patchValue({ planCode: this.plans[0].code });
    }
  }
  tenantForm = this.fb.group({
    name: ['', Validators.required],
    slug: ['', [Validators.required, Validators.minLength(3), Validators.pattern(/^[a-z0-9-]+$/)]],
    planCode: ['', Validators.required],
    adminEmail: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    firstName: ['', Validators.required],
    lastName: ['', Validators.required]
  });

  onSubmit(): void {
    Object.keys(this.tenantForm.controls).forEach(key => {
      const control = this.tenantForm.get(key);
      control?.markAsTouched();
    });
    if (this.tenantForm.valid) {
      this.submitForm.emit(this.tenantForm.value as CreateTenantFormData);
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.tenantForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}

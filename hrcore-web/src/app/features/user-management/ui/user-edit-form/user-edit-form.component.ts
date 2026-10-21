import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UpdateTenantUserRequest, TenantUserResponse } from '../../../../entities/user';
import { EmployeeSummary } from '../../../../entities/employees/model/employees.model';

@Component({
  selector: 'app-user-edit-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="userForm" (ngSubmit)="onSubmit()" class="flex min-h-0 flex-1 flex-col overflow-hidden" novalidate>
      <div class="flex-1 space-y-5 overflow-y-auto p-6">
        <div class="rounded-2xl border border-border/80 bg-muted/25 p-5">
          <div class="grid gap-4 sm:grid-cols-2">
            <div class="space-y-1.5">
              <label for="firstNameEdit" class="block text-xs font-bold text-accent-foreground">Nombre *</label>
              <input
                id="firstNameEdit"
                type="text"
                formControlName="firstName"
                class="flex h-11 w-full rounded-xl border border-border bg-card px-3.5 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
                [class.border-destructive]="isFieldInvalid('firstName')"
                placeholder="Ej: Carlos">
              <span *ngIf="isFieldInvalid('firstName')" class="block text-xs font-medium text-destructive">El nombre es obligatorio.</span>
            </div>

            <div class="space-y-1.5">
              <label for="lastNameEdit" class="block text-xs font-bold text-accent-foreground">Apellido *</label>
              <input
                id="lastNameEdit"
                type="text"
                formControlName="lastName"
                class="flex h-11 w-full rounded-xl border border-border bg-card px-3.5 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
                [class.border-destructive]="isFieldInvalid('lastName')"
                placeholder="Ej: Rojas">
              <span *ngIf="isFieldInvalid('lastName')" class="block text-xs font-medium text-destructive">El apellido es obligatorio.</span>
            </div>

            <div class="space-y-1.5">
              <label for="emailEdit" class="block text-xs font-bold text-accent-foreground">Correo electrónico *</label>
              <input
                id="emailEdit"
                type="email"
                formControlName="email"
                class="flex h-11 w-full rounded-xl border border-border bg-card px-3.5 text-sm text-accent-foreground outline-none transition-colors placeholder:text-muted-foreground focus:border-primary"
                [class.border-destructive]="isFieldInvalid('email')"
                placeholder="usuario@empresa.com">
              <span *ngIf="isFieldInvalid('email')" class="block text-xs font-medium text-destructive">Ingresa un correo válido.</span>
            </div>

            <!-- Vinculación con empleado -->
            <div class="space-y-1.5">
              <label for="employeeEdit" class="block text-xs font-bold text-accent-foreground">
                Empleado vinculado
              </label>
              <select
                id="employeeEdit"
                formControlName="employeeId"
                class="flex h-11 w-full rounded-xl border border-border bg-card px-3.5 text-sm text-accent-foreground outline-none transition-colors focus:border-primary">
                <option [ngValue]="null">Sin vincular a empleado</option>
                @for (emp of availableEmployees; track emp.id) {
                  <option [ngValue]="emp.id">
                    {{ emp.employeeNumber }} — {{ emp.firstName }} {{ emp.lastName }}
                  </option>
                }
              </select>
              <p class="text-[11px] text-muted-foreground">
                Asocia este usuario a su ficha laboral o desvincula el registro.
              </p>
            </div>
          </div>
        </div>
      </div>

      <div class="app-modal-footer">
        <button
          type="button"
          (click)="onCancel()"
          [disabled]="status === 'loading'"
          class="cursor-pointer rounded-full border border-border bg-card px-5 py-2.5 text-sm font-semibold text-muted-foreground transition-colors hover:bg-muted hover:text-foreground disabled:cursor-not-allowed disabled:opacity-50">
          Cancelar
        </button>
        <button
          type="submit"
          [disabled]="status === 'loading'"
          class="cursor-pointer inline-flex items-center gap-2 rounded-full bg-primary px-6 py-2.5 text-sm font-black text-primary-foreground shadow-sm transition-all hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-50">
          <ng-container *ngIf="status !== 'loading'; else loadingSpinner">
            Guardar cambios
          </ng-container>
          <ng-template #loadingSpinner>
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-primary-foreground" fill="none" viewBox="0 0 24 24">
              <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4"></circle>
              <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
            </svg>
            Guardando...
          </ng-template>
        </button>
      </div>
    </form>
  `
})
export class UserEditFormComponent implements OnChanges {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() user!: TenantUserResponse;
  @Input() availableEmployees: EmployeeSummary[] = [];

  @Output() formSubmit = new EventEmitter<UpdateTenantUserRequest>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  userForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    employeeId: [null]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (this.user) {
      this.userForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email,
        employeeId: this.user.employeeId ?? null
      });
    }
  }

  onSubmit(): void {
    if (this.userForm.valid && this.status !== 'loading') {
      const val = this.userForm.value;
      const first = (val.firstName || '').trim();
      const last = (val.lastName || '').trim();
      const payload: UpdateTenantUserRequest = {
        email: (val.email || '').trim().toLowerCase(),
        firstName: first,
        lastName: last,
        displayName: `${first} ${last}`.trim(),
        employeeId: val.employeeId || null
      };

      this.formSubmit.emit(payload);
    } else {
      this.userForm.markAllAsTouched();
    }
  }

  onCancel(): void {
    if (this.status !== 'loading') {
      this.cancel.emit();
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.userForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
}

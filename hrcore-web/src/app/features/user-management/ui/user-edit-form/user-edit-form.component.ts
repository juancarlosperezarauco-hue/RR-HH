import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { UpdateTenantUserRequest, TenantUserResponse } from '../../../../entities/user';

@Component({
  selector: 'app-user-edit-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  template: `
    <form [formGroup]="userForm" (ngSubmit)="onSubmit()" class="space-y-5">
      <div class="grid gap-5 sm:grid-cols-2">
        <div class="space-y-2">
          <label for="firstNameEdit" class="text-sm font-semibold text-[#567157]">Nombre</label>
          <input
            id="firstNameEdit"
            type="text"
            formControlName="firstName"
            class="flex h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-3 py-2 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white"
            [class.border-red-300]="isFieldInvalid('firstName')"
            placeholder="Ej: Carlos">
          <span *ngIf="isFieldInvalid('firstName')" class="text-xs text-red-600">El nombre es requerido.</span>
        </div>

        <div class="space-y-2">
          <label for="lastNameEdit" class="text-sm font-semibold text-[#567157]">Apellido</label>
          <input
            id="lastNameEdit"
            type="text"
            formControlName="lastName"
            class="flex h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-3 py-2 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white"
            [class.border-red-300]="isFieldInvalid('lastName')"
            placeholder="Ej: Rojas">
          <span *ngIf="isFieldInvalid('lastName')" class="text-xs text-red-600">El apellido es requerido.</span>
        </div>
      </div>

      <div class="space-y-2">
        <label for="emailEdit" class="text-sm font-semibold text-[#567157]">Correo electrónico</label>
        <input
          id="emailEdit"
          type="email"
          formControlName="email"
          class="flex h-11 w-full rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-3 py-2 text-sm text-[#1B5E20] outline-none transition-colors placeholder:text-[#9AA99A] focus:border-[#2E7D32] focus:bg-white"
          [class.border-red-300]="isFieldInvalid('email')"
          placeholder="usuario@empresa.com">
        <span *ngIf="isFieldInvalid('email')" class="text-xs text-red-600">Ingresa un correo válido.</span>
      </div>

      <div class="app-modal-footer mt-2 border-t border-[#E8F2E2] pt-5">
        <button
          type="button"
          (click)="onCancel()"
          [disabled]="status === 'loading'"
          class="cursor-pointer rounded-full border border-[#DDEED8] bg-white px-5 py-2.5 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-50">
          Cancelar
        </button>
        <button
          type="submit"
          [disabled]="status === 'loading'"
          class="cursor-pointer inline-flex items-center gap-2 rounded-full bg-[#2E7D32] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#256428] disabled:cursor-not-allowed disabled:opacity-50">
          <ng-container *ngIf="status !== 'loading'; else loadingSpinner">
            Guardar cambios
          </ng-container>
          <ng-template #loadingSpinner>
            <svg class="animate-spin -ml-1 mr-2 h-4 w-4 text-white" fill="none" viewBox="0 0 24 24">
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
  
  @Output() formSubmit = new EventEmitter<UpdateTenantUserRequest>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  userForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]]
  });

  ngOnChanges(changes: SimpleChanges): void {
    if (this.user) {
      this.userForm.patchValue({
        firstName: this.user.firstName,
        lastName: this.user.lastName,
        email: this.user.email
      });
    }
  }

  onSubmit(): void {
    if (this.userForm.valid && this.status !== 'loading') {
      this.formSubmit.emit(this.userForm.value);
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

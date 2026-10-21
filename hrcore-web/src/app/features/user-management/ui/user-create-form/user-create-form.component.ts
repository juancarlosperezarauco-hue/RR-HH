import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { CreateTenantUserRequest } from '../../../../entities/user';
import { TenantRoleResponse } from '../../../../entities/access';
import { EmployeeSummary } from '../../../../entities/employees/model/employees.model';

@Component({
  selector: 'app-user-create-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './user-create-form.component.html'
})
export class UserCreateFormComponent {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() error: string | null = null;
  @Input() canAssignRoles = false;
  @Input() availableRoles: TenantRoleResponse[] = [];
  @Input() availableEmployees: EmployeeSummary[] = [];

  @Output() formSubmit = new EventEmitter<CreateTenantUserRequest>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  showPassword = false;

  userForm: FormGroup = this.fb.group({
    displayName: [''],
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]],
    status: ['ACTIVE', [Validators.required]],
    employeeId: [null],
    roleIds: [[]]
  });

  onEmployeeSelect(event: Event): void {
    const selectedId = (event.target as HTMLSelectElement).value;
    if (!selectedId) {
      this.userForm.patchValue({ employeeId: null });
      return;
    }

    this.userForm.patchValue({ employeeId: selectedId });
    const emp = this.availableEmployees.find((e) => e.id === selectedId);
    if (emp) {
      const first = emp.firstName || '';
      const last = emp.lastName || '';
      this.userForm.patchValue({
        firstName: first,
        lastName: last,
        displayName: `${first} ${last}`.trim()
      });
    }
  }

  onDisplayNameInput(value: string): void {
    const trimmed = (value || '').trim();
    if (trimmed) {
      const parts = trimmed.split(/\s+/);
      const first = parts[0] || '';
      const last = parts.slice(1).join(' ');
      this.userForm.patchValue({ firstName: first, lastName: last }, { emitEvent: false });
    }
  }

  onSubmit(): void {
    if (this.userForm.valid && this.status !== 'loading') {
      const val = this.userForm.value;
      const first = (val.firstName || '').trim();
      const last = (val.lastName || '').trim();
      const display = (val.displayName || '').trim() || `${first} ${last}`.trim();

      const payload: CreateTenantUserRequest = {
        email: (val.email || '').trim().toLowerCase(),
        password: val.password,
        firstName: first,
        lastName: last,
        displayName: display,
        status: val.status,
        employeeId: val.employeeId || null,
        roleIds: val.roleIds || []
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

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  toggleRole(roleId: string, event: Event): void {
    const selected = new Set<string>(this.userForm.get('roleIds')?.value ?? []);
    (event.target as HTMLInputElement).checked ? selected.add(roleId) : selected.delete(roleId);
    this.userForm.get('roleIds')?.setValue([...selected]);
  }
}

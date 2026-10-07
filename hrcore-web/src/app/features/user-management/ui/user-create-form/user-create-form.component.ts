import { Component, EventEmitter, inject, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { CreateTenantUserRequest } from '../../../../entities/user';

@Component({
  selector: 'app-user-create-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule],
  templateUrl: './user-create-form.component.html'
})
export class UserCreateFormComponent {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  
  @Output() formSubmit = new EventEmitter<CreateTenantUserRequest>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);
  showPassword = false;

  userForm: FormGroup = this.fb.group({
    firstName: ['', [Validators.required]],
    lastName: ['', [Validators.required]],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

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

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}

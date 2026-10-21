import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { trigger, transition, style, animate } from '@angular/animations';
import { PublicSignupRequest } from '../../../../entities/tenant/model/public-signup-request.model';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-signup-form',
  standalone: true,
  imports: [
    CommonModule, 
    ReactiveFormsModule, 
    RouterModule, 
    LucideAngularModule,
  ],
  templateUrl: './signup-form.component.html',
  animations: [
    trigger('fadeSlideInOut', [
      transition(':enter', [
        style({ opacity: 0, transform: 'translateY(-10px)' }),
        animate('300ms ease-out', style({ opacity: 1, transform: 'translateY(0)' }))
      ]),
      transition(':leave', [
        animate('300ms ease-in', style({ opacity: 0, transform: 'translateY(-10px)' }))
      ])
    ])
  ]
})
export class SignupFormComponent {
  @Input() loading = false;
  @Input() error: string | null = null;
  @Input() success = false;
  @Input() redirectingToPayment = false;
  @Input() selectedPlanCode = 'DEMO';
  @Input() selectedBillingInterval: 'MONTHLY' | 'YEARLY' = 'MONTHLY';
  
  @Output() formSubmit = new EventEmitter<PublicSignupRequest>();

  showPassword = false;
  submitted = false;

  signupForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.signupForm = this.fb.group({
      companyName: ['', [Validators.required, Validators.minLength(3)]],
      tenantSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      adminEmail: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]]
    });

    this.signupForm.get('companyName')?.valueChanges.subscribe((value: string) => {
      const slugCtrl = this.signupForm.get('tenantSlug');
      if (slugCtrl && !slugCtrl.dirty) {
        const suggested = (value || '')
          .toLowerCase()
          .normalize('NFD')
          .replace(/[\u0300-\u036f]/g, '')
          .replace(/[^a-z0-9]+/g, '-')
          .replace(/^-+|-+$/g, '')
          .slice(0, 40);
        slugCtrl.setValue(suggested, { emitEvent: false });
      }
    });
  }

  onSubmit(): void {
    this.submitted = true;
    if (this.signupForm.valid && !this.loading) {
      this.formSubmit.emit(this.signupForm.value);
    } else {
      this.signupForm.markAllAsTouched();
    }
  }

  // Helpers para la UI
  isFieldInvalid(fieldName: string): boolean {
    const field = this.signupForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched || this.submitted) : false;
  }

  getFieldError(fieldName: string): string {
    const field = this.signupForm.get(fieldName);
    if (!field || !field.errors) return '';
    if (field.errors['required']) {
      const names: Record<string, string> = {
        companyName: 'El nombre de la empresa es obligatorio.',
        tenantSlug: 'El identificador único es obligatorio.',
        firstName: 'El nombre es obligatorio.',
        lastName: 'El apellido es obligatorio.',
        adminEmail: 'El correo electrónico es obligatorio.',
        password: 'La contraseña es obligatoria.'
      };
      return names[fieldName] || 'Este campo es obligatorio.';
    }
    if (field.errors['minlength']) {
      if (fieldName === 'companyName') return 'Debe tener al menos 3 caracteres.';
      if (fieldName === 'password') return 'Debe tener al menos 8 caracteres.';
      return 'Ingresa al menos 2 caracteres.';
    }
    if (field.errors['email']) return 'Ingresa un formato de correo válido.';
    if (field.errors['pattern']) return 'Usa solo letras minúsculas, números y guiones.';
    return 'Verifica este campo.';
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  isPaidSignup(): boolean {
    const code = this.selectedPlanCode.trim().toUpperCase();
    return code !== 'DEMO' && code !== 'ENTERPRISE';
  }

  shouldShowSuccessCard(): boolean {
    return this.success && !this.redirectingToPayment;
  }
}

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

  signupForm: FormGroup;

  constructor(private fb: FormBuilder) {
    this.signupForm = this.fb.group({
      companyName: ['', [Validators.required, Validators.minLength(3)]],
      tenantSlug: ['', [Validators.required, Validators.pattern(/^[a-z0-9-]+$/)]],
      adminEmail: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(8)]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.signupForm.valid && !this.loading) {
      this.formSubmit.emit(this.signupForm.value);
    } else {
      this.signupForm.markAllAsTouched();
    }
  }

  // Helpers para la UI
  isFieldInvalid(fieldName: string): boolean {
    const field = this.signupForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
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

import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { PublicPaidSignupRequest, PublicSignupRequest } from '../../entities/tenant';
import { SignupFormComponent, SignupUseCase } from '../../features/onboarding';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';

type BillingInterval = 'MONTHLY' | 'YEARLY';

@Component({
  selector: 'app-onboarding-page',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    PublicNavbarComponent,
    PublicFooterComponent,
    SignupFormComponent
  ],
  template: `
    <div class="min-h-screen bg-[#F7FBF3]">
      <app-public-navbar />

      <main class="px-4 py-12 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-7xl">
          @if (ready()) {
            <app-signup-form
              [loading]="signupUseCase.loading()"
              [error]="signupUseCase.error()"
              [success]="signupUseCase.success()"
              [redirectingToPayment]="signupUseCase.redirectingToPayment()"
              [selectedPlanCode]="selectedPlanCode()"
              [selectedBillingInterval]="selectedBillingInterval()"
              (formSubmit)="handleSignup($event)"
            />
          }
        </div>
      </main>

      <app-public-footer />
    </div>
  `
})
export class OnboardingPageComponent implements OnInit {
  readonly signupUseCase = inject(SignupUseCase);

  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly ready = signal(false);
  readonly selectedPlanCode = signal('');
  readonly selectedBillingInterval = signal<BillingInterval>('MONTHLY');

  readonly isPaidSignup = computed(() => {
    const plan = this.selectedPlanCode().trim().toUpperCase();
    return plan !== 'DEMO' && plan !== 'FREE' && plan !== 'ENTERPRISE';
  });

  async ngOnInit(): Promise<void> {
    this.signupUseCase.resetState();

    const query = this.route.snapshot.queryParamMap;
    const rawPlan = query.get('plan');

    if (!rawPlan || !rawPlan.trim()) {
      await this.router.navigate(['/prices'], { replaceUrl: true });
      return;
    }

    const plan = this.normalizePlanCode(rawPlan);
    const billing = this.normalizeBillingInterval(query.get('billing'));

    this.selectedPlanCode.set(plan);
    this.selectedBillingInterval.set(billing);
    this.ready.set(true);
  }

  handleSignup(request: PublicSignupRequest): void {
    const cleanRequest: PublicSignupRequest = {
      companyName: request.companyName.trim(),
      tenantSlug: request.tenantSlug.trim().toLowerCase(),
      adminEmail: request.adminEmail.trim().toLowerCase(),
      password: request.password,
      firstName: request.firstName.trim(),
      lastName: request.lastName.trim()
    };

    if (this.isPaidSignup()) {
      const paidRequest: PublicPaidSignupRequest = {
        ...cleanRequest,
        planCode: this.selectedPlanCode(),
        billingInterval: this.selectedBillingInterval()
      };

      this.signupUseCase.signupPaid(paidRequest);
      return;
    }

    this.signupUseCase.signup(cleanRequest);
  }

  private normalizePlanCode(value: string): string {
    return value.trim().toUpperCase();
  }

  private normalizeBillingInterval(value: string | null): BillingInterval {
    const billing = (value || 'MONTHLY').trim().toUpperCase();
    return billing === 'YEARLY' ? 'YEARLY' : 'MONTHLY';
  }
}

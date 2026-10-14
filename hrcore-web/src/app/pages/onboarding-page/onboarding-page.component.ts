import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { firstValueFrom, timeout } from 'rxjs';
import { PublicBillingService, PublicBillingPlan } from '../../entities/billing';
import { PublicPaidSignupRequest, PublicSignupRequest } from '../../entities/tenant';
import { SignupFormComponent, SignupUseCase } from '../../features/onboarding';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';

type BillingInterval = 'MONTHLY' | 'YEARLY';

@Component({
  selector: 'app-onboarding-page',
  standalone: true,
  imports: [CommonModule, RouterModule, PublicNavbarComponent, PublicFooterComponent, SignupFormComponent],
  template: `
    <div class="hrcore-brand-theme min-h-screen bg-background">
      <app-public-navbar />
      <main class="px-4 py-12 sm:px-6 lg:px-8">
        <div class="mx-auto max-w-7xl">
          @if (loadingPlan()) {
            <p role="status" class="text-center text-muted-foreground">Validando el plan seleccionado...</p>
          } @else if (planError()) {
            <div role="alert" class="mx-auto max-w-xl rounded-xl border border-destructive/20 p-6 text-center">
              <p class="text-destructive">{{ planError() }}</p>
              <a routerLink="/prices" class="mt-4 inline-block font-semibold text-primary underline">Volver a los planes</a>
            </div>
          } @else if (ready()) {
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
  private readonly billing = inject(PublicBillingService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);

  readonly loadingPlan = signal(true);
  readonly planError = signal<string | null>(null);
  readonly ready = signal(false);
  readonly selectedPlanCode = signal('');
  readonly selectedBillingInterval = signal<BillingInterval>('MONTHLY');
  readonly isPaidSignup = computed(() => this.selectedPlanCode() !== 'DEMO');

  async ngOnInit(): Promise<void> {
    this.signupUseCase.resetState();
    const query = this.route.snapshot.queryParamMap;
    const planCode = (query.get('plan') ?? '').trim().toUpperCase();
    const billingInterval: BillingInterval = query.get('billing')?.trim().toUpperCase() === 'YEARLY' ? 'YEARLY' : 'MONTHLY';

    if (!planCode) {
      this.loadingPlan.set(false);
      await this.router.navigate(['/prices'], { replaceUrl: true });
      return;
    }

    try {
      const response = await firstValueFrom(this.billing.listPublicPlans().pipe(timeout(15000)));
      const plans: PublicBillingPlan[] = response.success && Array.isArray(response.data) ? response.data : [];
      const plan = plans.find(p => p.code.toUpperCase() === planCode && p.active && p.publicVisible);
      if (!plan) {
        this.planError.set('El plan seleccionado no está disponible para registro.');
        return;
      }
      if (planCode === 'DEMO' && plan.planType !== 'DEMO') {
        this.planError.set('La configuración del plan DEMO no es válida.');
        return;
      }
      if (planCode !== 'DEMO' && (plan.planType !== 'PAID' || !plan.availableForCheckout ||
        (billingInterval === 'MONTHLY' ? plan.monthlyAmount : plan.yearlyAmount) == null)) {
        this.planError.set('Este plan no admite registro automático. Selecciona otro plan disponible.');
        return;
      }
      this.selectedPlanCode.set(plan.code);
      this.selectedBillingInterval.set(billingInterval);
      this.ready.set(true);
    } catch {
      this.planError.set('No se pudo verificar el plan. Inténtalo nuevamente desde la página de precios.');
    } finally {
      this.loadingPlan.set(false);
    }
  }

  handleSignup(request: PublicSignupRequest): void {
    if (!this.ready() || this.signupUseCase.loading()) return;
    const cleanRequest: PublicSignupRequest = {
      companyName: request.companyName.trim(), tenantSlug: request.tenantSlug.trim().toLowerCase(),
      adminEmail: request.adminEmail.trim().toLowerCase(), password: request.password,
      firstName: request.firstName.trim(), lastName: request.lastName.trim()
    };
    if (this.isPaidSignup()) {
      const paidRequest: PublicPaidSignupRequest = { ...cleanRequest, planCode: this.selectedPlanCode(), billingInterval: this.selectedBillingInterval() };
      void this.signupUseCase.signupPaid(paidRequest);
    } else {
      void this.signupUseCase.signup(cleanRequest);
    }
  }
}

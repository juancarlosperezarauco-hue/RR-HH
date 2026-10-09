import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { PublicPaidSignupRequest, PublicSignupRequest, TenantService } from '../../../entities/tenant';

export interface SignupState {
  loading: boolean;
  error: string | null;
  success: boolean;
  redirectingToPayment: boolean;
}

export const CHECKOUT_PENDING_KEY = 'hrcore.checkout.pending';

@Injectable({
  providedIn: 'root'
})
export class SignupUseCase {
  private readonly tenantService = inject(TenantService);
  private readonly state = signal<SignupState>({
    loading: false,
    error: null,
    success: false,
    redirectingToPayment: false
  });

  readonly loading = computed(() => this.state().loading);
  readonly error = computed(() => this.state().error);
  readonly success = computed(() => this.state().success);
  readonly redirectingToPayment = computed(() => this.state().redirectingToPayment);

  async signup(request: PublicSignupRequest): Promise<void> {
    this.state.set({ loading: true, error: null, success: false, redirectingToPayment: false });

    try {
      const response = await firstValueFrom(this.tenantService.publicSignup(request));

      if (response.success && response.data) {
        this.state.set({ loading: false, error: null, success: true, redirectingToPayment: false });
        return;
      }

      this.state.set({
        loading: false,
        error: response.message || 'No se pudo crear el tenant demo',
        success: false,
        redirectingToPayment: false
      });
    } catch (err: any) {
      this.state.set({
        loading: false,
        error: err.error?.message || err.message || 'Error al conectar con el servidor',
        success: false,
        redirectingToPayment: false
      });
    }
  }

  async signupPaid(request: PublicPaidSignupRequest): Promise<void> {
    this.state.set({ loading: true, error: null, success: false, redirectingToPayment: false });

    try {
      const response = await firstValueFrom(this.tenantService.publicPaidSignup(request));

      if (response.success && response.data) {
        this.state.set({
          loading: false,
          error: null,
          success: true,
          redirectingToPayment: false
        });
        return;
      }

      this.state.set({
        loading: false,
        error: response.message || 'No se pudo crear la organización con el plan seleccionado',
        success: false,
        redirectingToPayment: false
      });
    } catch (err: any) {
      this.state.set({
        loading: false,
        error: err.error?.message || err.message || 'Error al conectar con el servidor',
        success: false,
        redirectingToPayment: false
      });
    }
  }

  resetState(): void {
    this.state.set({ loading: false, error: null, success: false, redirectingToPayment: false });
  }
}

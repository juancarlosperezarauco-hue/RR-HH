import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import {
  BillingInterval,
  TenantBillingPlan,
  TenantBillingService,
  TenantCheckoutActivationStatusResponse,
  TenantCurrentSubscription,
  TenantUpgradeCheckoutPending,
  TenantUpgradeCheckoutStorageService
} from '../../../entities/billing';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';

interface TenantSubscriptionState {
  loading: boolean;
  checkoutLoading: boolean;
  statusLoading: boolean;
  current: TenantCurrentSubscription | null;
  plans: TenantBillingPlan[];
  activationStatus: TenantCheckoutActivationStatusResponse | null;
  error: string | null;
}

@Injectable({
  providedIn: 'root'
})
export class TenantSubscriptionUseCase {
  private readonly billingService = inject(TenantBillingService);
  private readonly storage = inject(TenantUpgradeCheckoutStorageService);
  private readonly authStorage = inject(AuthStorageService);

  private readonly state = signal<TenantSubscriptionState>({
    loading: false,
    checkoutLoading: false,
    statusLoading: false,
    current: null,
    plans: [],
    activationStatus: null,
    error: null
  });

  readonly loading = computed(() => this.state().loading);
  readonly checkoutLoading = computed(() => this.state().checkoutLoading);
  readonly statusLoading = computed(() => this.state().statusLoading);
  readonly current = computed(() => this.state().current);
  readonly plans = computed(() => this.state().plans);
  readonly activationStatus = computed(() => this.state().activationStatus);
  readonly error = computed(() => this.state().error);

  async load(): Promise<void> {
    this.state.update((state) => ({
      ...state,
      loading: true,
      error: null
    }));

    try {
      const [currentResponse, plansResponse] = await Promise.all([
        firstValueFrom(this.billingService.getCurrentSubscription()),
        firstValueFrom(this.billingService.listPlans())
      ]);

      this.state.update((state) => ({
        ...state,
        loading: false,
        current: currentResponse.data ?? null,
        plans: plansResponse.data ?? [],
        error: null
      }));
    } catch (err: any) {
      this.state.update((state) => ({
        ...state,
        loading: false,
        error: err.error?.message || err.message || 'No se pudo cargar la suscripción'
      }));
    }
  }

  async createCheckout(planCode: string, billingInterval: BillingInterval): Promise<void> {
    this.state.update((state) => ({
      ...state,
      checkoutLoading: true,
      error: null
    }));

    try {
      const response = await firstValueFrom(
        this.billingService.createCheckoutSession({
          planCode,
          billingInterval
        })
      );

      if (!response.success || !response.data?.checkoutUrl) {
        this.state.update((state) => ({
          ...state,
          checkoutLoading: false,
          error: response.message || 'No se pudo crear el checkout'
        }));
        return;
      }

      this.storage.save({
        stripeSessionId: response.data.stripeSessionId,
        tenantSlug: this.authStorage.getTenantSlug() || '',
        planCode: response.data.planCode,
        billingInterval: response.data.billingInterval,
        checkoutUrl: response.data.checkoutUrl,
        createdAt: new Date().toISOString()
      });

      if (!this.isTrustedCheckoutUrl(response.data.checkoutUrl)) {
        this.state.update((state) => ({ ...state, checkoutLoading: false, error: 'La URL de checkout no es válida.' }));
        this.storage.clear();
        return;
      }

      window.location.assign(response.data.checkoutUrl);
    } catch (err: any) {
      this.state.update((state) => ({
        ...state,
        checkoutLoading: false,
        error: err.error?.message || err.message || 'No se pudo iniciar el upgrade'
      }));
    }
  }

  async checkActivationStatus(stripeSessionId: string): Promise<void> {
    this.state.update((state) => ({
      ...state,
      statusLoading: true,
      error: null
    }));

    try {
      const response = await firstValueFrom(this.billingService.getCheckoutResult(stripeSessionId));

      if (!response.success || !response.data) {
        this.state.update((state) => ({
          ...state,
          statusLoading: false,
          error: response.message || 'No se pudo verificar la activación'
        }));
        return;
      }

      const rawStatus = response.data.status.trim().toUpperCase();
      const activationStatus = this.mapActivationStatus(rawStatus);
      const activation: TenantCheckoutActivationStatusResponse = {
        checkoutSessionId: response.data.checkoutSessionId,
        stripeSessionId: response.data.stripeSessionId,
        tenantSlug: this.authStorage.getTenantSlug(),
        planCode: response.data.planCode,
        billingInterval: response.data.billingInterval,
        checkoutStatus: rawStatus,
        activationStatus,
        paid: activationStatus === 'ACTIVE' || rawStatus === 'COMPLETED',
        active: activationStatus === 'ACTIVE',
        failed: activationStatus === 'FAILED',
        pending: activationStatus === 'PENDING',
        message: this.mapActivationMessage(activationStatus),
        expiresAt: response.data.expiresAt
      };

      this.state.update((state) => ({
        ...state,
        statusLoading: false,
        activationStatus: activation,
        error: null
      }));

      if (activation.active) {
        this.storage.clear();
      }
    } catch (err: any) {
      this.state.update((state) => ({
        ...state,
        statusLoading: false,
        error: err.error?.message || err.message || 'No se pudo verificar la activación'
      }));
    }
  }

  pendingCheckout(): TenantUpgradeCheckoutPending | null {
    return this.storage.get();
  }

  clearPendingCheckout(): void {
    this.storage.clear();
  }

  private mapActivationStatus(status: string): 'PENDING' | 'ACTIVE' | 'FAILED' {
    if (status === 'COMPLETED') {
      return 'ACTIVE';
    }

    if (status === 'EXPIRED' || status === 'FAILED' || status === 'CANCELLED') {
      return 'FAILED';
    }

    return 'PENDING';
  }

  private mapActivationMessage(status: 'PENDING' | 'ACTIVE' | 'FAILED'): string {
    if (status === 'ACTIVE') {
      return 'Pago confirmado y plan activado.';
    }

    if (status === 'FAILED') {
      return 'El pago venció o no pudo completarse.';
    }

    return 'Pago recibido; la activación está pendiente.';
  }

  private isTrustedCheckoutUrl(value: string): boolean {
    try {
      const url = new URL(value);
      return url.protocol === 'https:' && !url.username && !url.password && !['localhost', '127.0.0.1'].includes(url.hostname);
    } catch {
      return false;
    }
  }
}

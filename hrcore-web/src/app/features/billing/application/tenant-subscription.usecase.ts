import { computed, inject, Injectable, signal } from '@angular/core';
import { firstValueFrom, timeout } from 'rxjs';
import { BillingInterval, TenantBillingPlan, TenantBillingService, TenantCheckoutActivationStatusResponse, TenantCurrentSubscription, TenantUpgradeCheckoutPending, TenantUpgradeCheckoutStorageService } from '../../../entities/billing';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';

interface TenantSubscriptionState { loading: boolean; checkoutLoading: boolean; statusLoading: boolean; current: TenantCurrentSubscription | null; plans: TenantBillingPlan[]; activationStatus: TenantCheckoutActivationStatusResponse | null; error: string | null; }
type ActivationStatus = 'PENDING' | 'ACTIVE' | 'FAILED';

@Injectable({ providedIn: 'root' })
export class TenantSubscriptionUseCase {
  private readonly billingService = inject(TenantBillingService);
  private readonly storage = inject(TenantUpgradeCheckoutStorageService);
  private readonly authStorage = inject(AuthStorageService);
  private lastCheckoutSessionId: string | null = null;
  private readonly state = signal<TenantSubscriptionState>({ loading: false, checkoutLoading: false, statusLoading: false, current: null, plans: [], activationStatus: null, error: null });
  readonly loading = computed(() => this.state().loading);
  readonly checkoutLoading = computed(() => this.state().checkoutLoading);
  readonly statusLoading = computed(() => this.state().statusLoading);
  readonly current = computed(() => this.state().current);
  readonly plans = computed(() => this.state().plans);
  readonly activationStatus = computed(() => this.state().activationStatus);
  readonly error = computed(() => this.state().error);

  async load(): Promise<void> {
    const tenantSlug = this.authStorage.getTenantSlug();
    this.state.update(s => ({ ...s, loading: true, error: null }));
    try {
      const [current, plans] = await Promise.all([firstValueFrom(this.billingService.getCurrentSubscription().pipe(timeout(15000))), firstValueFrom(this.billingService.listPlans().pipe(timeout(15000)))]);
      if (tenantSlug !== this.authStorage.getTenantSlug()) return;
      if (!current.success || !plans.success || !current.data || !Array.isArray(plans.data)) throw new Error('El servidor no devolvió la información de suscripción esperada.');
      this.state.update(s => ({ ...s, current: current.data, plans: plans.data, error: null }));
    } catch (err: unknown) {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, error: this.errorMessage(err, 'No se pudo cargar la suscripción') }));
    } finally {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, loading: false }));
    }
  }

  async createCheckout(planCode: string, billingInterval: BillingInterval): Promise<void> {
    if (this.state().checkoutLoading) return;
    const tenantSlug = this.authStorage.getTenantSlug();
    if (!tenantSlug) { this.state.update(s => ({ ...s, error: 'No hay tenant autenticado.' })); return; }
    this.state.update(s => ({ ...s, checkoutLoading: true, error: null }));
    try {
      const response = await firstValueFrom(this.billingService.createCheckoutSession({ planCode, billingInterval }).pipe(timeout(15000)));
      if (tenantSlug !== this.authStorage.getTenantSlug()) return;
      const checkout = response.data;
      if (!response.success || !checkout?.checkoutUrl || !checkout.stripeSessionId || !checkout.planCode || !this.isTrustedCheckoutUrl(checkout.checkoutUrl)) throw new Error('El servidor no devolvió un checkout válido.');
      this.storage.save({ stripeSessionId: checkout.stripeSessionId, tenantSlug, planCode: checkout.planCode, billingInterval: checkout.billingInterval, checkoutUrl: checkout.checkoutUrl, createdAt: new Date().toISOString() });
      window.location.assign(checkout.checkoutUrl);
    } catch (err: unknown) {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, error: this.errorMessage(err, 'No se pudo iniciar el checkout') }));
    } finally {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, checkoutLoading: false }));
    }
  }

  async checkActivationStatus(stripeSessionId: string): Promise<void> {
    if (!stripeSessionId || this.state().statusLoading) return;
    const tenantSlug = this.authStorage.getTenantSlug();
    if (!tenantSlug) return;
    if (this.lastCheckoutSessionId !== stripeSessionId) { this.lastCheckoutSessionId = stripeSessionId; this.state.update(s => ({ ...s, activationStatus: null })); }
    this.state.update(s => ({ ...s, statusLoading: true, error: null }));
    try {
      const checkoutResponse = await firstValueFrom(this.billingService.getCheckoutResult(stripeSessionId).pipe(timeout(15000)));
      if (tenantSlug !== this.authStorage.getTenantSlug()) return;
      if (!checkoutResponse.success || !checkoutResponse.data || checkoutResponse.data.stripeSessionId !== stripeSessionId) throw new Error('No se pudo verificar la sesión de checkout.');
      const checkout = checkoutResponse.data;
      const checkoutStatus = checkout.status.trim().toUpperCase();
      let activation: ActivationStatus = 'PENDING';
      let activeSubscription: TenantCurrentSubscription | null = null;
      let message = 'Esperando confirmación del pago.';
      if (['FAILED', 'CANCELLED', 'EXPIRED'].includes(checkoutStatus)) {
        activation = 'FAILED'; message = 'El checkout fue cancelado, venció o falló. No se confirmó un nuevo pago.';
      } else if (checkoutStatus === 'COMPLETED') {
        message = 'Pago procesado. Esperando confirmación del nuevo plan.';
        const subscriptionResponse = await firstValueFrom(this.billingService.getCurrentSubscription().pipe(timeout(15000)));
        if (tenantSlug !== this.authStorage.getTenantSlug()) return;
        const current = subscriptionResponse.success ? subscriptionResponse.data : null;
        if (current && current.currentSubscription && current.status === 'ACTIVE' && current.planCode.trim().toUpperCase() === checkout.planCode.trim().toUpperCase()) {
          activation = 'ACTIVE'; activeSubscription = current; message = 'Pago confirmado y nuevo plan activado.';
        }
      }
      if (tenantSlug !== this.authStorage.getTenantSlug()) return;
      const result: TenantCheckoutActivationStatusResponse = { checkoutSessionId: checkout.checkoutSessionId, stripeSessionId: checkout.stripeSessionId, tenantSlug, planCode: checkout.planCode, billingInterval: checkout.billingInterval, checkoutStatus, activationStatus: activation, paid: checkoutStatus === 'COMPLETED', active: activation === 'ACTIVE', failed: activation === 'FAILED', pending: activation === 'PENDING', message, expiresAt: checkout.expiresAt };
      this.state.update(s => ({ ...s, activationStatus: result, current: activeSubscription ?? s.current, error: null }));
      if (activation === 'ACTIVE' && this.storage.get()?.stripeSessionId === stripeSessionId) this.storage.clear();
    } catch (err: unknown) {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, error: this.errorMessage(err, 'No se pudo verificar el estado del checkout') }));
    } finally {
      if (tenantSlug === this.authStorage.getTenantSlug()) this.state.update(s => ({ ...s, statusLoading: false }));
    }
  }

  pendingCheckout(): TenantUpgradeCheckoutPending | null { return this.storage.get(); }
  clearPendingCheckout(): void { this.storage.clear(); }
  private isTrustedCheckoutUrl(value: string): boolean { try { const url = new URL(value); return url.protocol === 'https:' && !url.username && !url.password && !['localhost', '127.0.0.1'].includes(url.hostname); } catch { return false; } }
  private errorMessage(error: unknown, fallback: string): string { if (typeof error === 'object' && error !== null) { const item = error as { error?: { message?: string }; message?: string }; return item.error?.message || item.message || fallback; } return fallback; }
}

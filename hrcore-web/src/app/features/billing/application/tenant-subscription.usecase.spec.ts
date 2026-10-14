import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TenantBillingService, TenantUpgradeCheckoutStorageService } from '../../../entities/billing';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
import { TenantSubscriptionUseCase } from './tenant-subscription.usecase';

describe('TenantSubscriptionUseCase - confirmación del checkout', () => {
  let useCase: TenantSubscriptionUseCase;
  let billing: jasmine.SpyObj<TenantBillingService>;
  let checkoutStorage: jasmine.SpyObj<TenantUpgradeCheckoutStorageService>;
  const reply = (data: unknown) => ({ success: true, data, message: 'OK', timestamp: '' });
  const checkout = (status: string) => ({ checkoutSessionId: 'd6c489cc-e840-4b5f-8a1f-c40d4b51b7da', stripeSessionId: 'cs_test_123', status, planCode: 'PRO', billingInterval: 'MONTHLY', checkoutUrl: 'https://checkout.stripe.com/example', completedAt: status === 'COMPLETED' ? new Date().toISOString() : null, expiresAt: null });
  const subscription = (planCode: string, status: string) => ({ id: 'subscription', tenantId: 'tenant', tenantName: 'Thunder', tenantSlug: 'thunder', planId: 'plan-id', planCode, planName: planCode, planType: 'PAID', maxUsers: 25, maxRoles: 10, status, trial: false, currentSubscription: true, stripeSubscriptionId: 'sub_123', stripePriceId: null, billingInterval: 'MONTHLY', startedAt: new Date().toISOString(), expiresAt: null, currentPeriodStart: null, currentPeriodEnd: null, cancelAtPeriodEnd: false, cancelledAt: null, remainingDays: null, createdAt: new Date().toISOString(), updatedAt: new Date().toISOString() });

  beforeEach(() => {
    billing = jasmine.createSpyObj<TenantBillingService>('TenantBillingService', ['getCurrentSubscription', 'listPlans', 'createCheckoutSession', 'getCheckoutResult']);
    checkoutStorage = jasmine.createSpyObj<TenantUpgradeCheckoutStorageService>('TenantUpgradeCheckoutStorageService', ['get', 'save', 'clear']);
    checkoutStorage.get.and.returnValue({ stripeSessionId: 'cs_test_123', tenantSlug: 'thunder', planCode: 'PRO', billingInterval: 'MONTHLY', checkoutUrl: 'https://checkout.stripe.com/example', createdAt: new Date().toISOString() });
    TestBed.configureTestingModule({ providers: [TenantSubscriptionUseCase, { provide: TenantBillingService, useValue: billing }, { provide: TenantUpgradeCheckoutStorageService, useValue: checkoutStorage }, { provide: AuthStorageService, useValue: { getTenantSlug: () => 'thunder' } }] });
    useCase = TestBed.inject(TenantSubscriptionUseCase);
  });

  it('COMPLETED con plan antiguo permanece pendiente', async () => {
    billing.getCheckoutResult.and.returnValue(of(reply(checkout('COMPLETED'))) as any);
    billing.getCurrentSubscription.and.returnValue(of(reply(subscription('BASIC', 'ACTIVE'))) as any);
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.paid).toBeTrue();
    expect(useCase.activationStatus()?.active).toBeFalse();
    expect(useCase.activationStatus()?.pending).toBeTrue();
    expect(checkoutStorage.clear).not.toHaveBeenCalled();
  });

  it('solo confirma ACTIVE cuando /current contiene el nuevo plan', async () => {
    billing.getCheckoutResult.and.returnValue(of(reply(checkout('COMPLETED'))) as any);
    billing.getCurrentSubscription.and.returnValue(of(reply(subscription('PRO', 'ACTIVE'))) as any);
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.active).toBeTrue();
    expect(useCase.current()?.planCode).toBe('PRO');
    expect(checkoutStorage.clear).toHaveBeenCalledTimes(1);
  });

  it('checkout FAILED no implica pago recibido ni consulta /current', async () => {
    billing.getCheckoutResult.and.returnValue(of(reply(checkout('FAILED'))) as any);
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.paid).toBeFalse();
    expect(useCase.activationStatus()?.failed).toBeTrue();
    expect(billing.getCurrentSubscription).not.toHaveBeenCalled();
  });

  it('PENDING espera y no confirma plan', async () => {
    billing.getCheckoutResult.and.returnValue(of(reply(checkout('OPEN'))) as any);
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.pending).toBeTrue();
    expect(useCase.activationStatus()?.active).toBeFalse();
    expect(billing.getCurrentSubscription).not.toHaveBeenCalled();
  });

  it('al consultar otro checkout no conserva el ACTIVE anterior', async () => {
    billing.getCheckoutResult.and.returnValues(of(reply(checkout('COMPLETED'))) as any, of(reply({ ...checkout('OPEN'), stripeSessionId: 'cs_test_other' })) as any);
    billing.getCurrentSubscription.and.returnValue(of(reply(subscription('PRO', 'ACTIVE'))) as any);
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.active).toBeTrue();
    await useCase.checkActivationStatus('cs_test_other');
    expect(useCase.activationStatus()?.active).toBeFalse();
    expect(useCase.activationStatus()?.pending).toBeTrue();
  });

  it('errores al consultar /current no generan falsos positivos', async () => {
    billing.getCheckoutResult.and.returnValue(of(reply(checkout('COMPLETED'))) as any);
    billing.getCurrentSubscription.and.returnValue(throwError(() => new Error('Servidor no disponible')));
    await useCase.checkActivationStatus('cs_test_123');
    expect(useCase.activationStatus()?.active ?? false).toBeFalse();
    expect(useCase.error()).toContain('Servidor no disponible');
    expect(checkoutStorage.clear).not.toHaveBeenCalled();
  });
});

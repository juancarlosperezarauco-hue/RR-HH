import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { PlatformTenantTableComponent } from './platform-tenant-table.component';
import { PlatformTenant } from '../../../../entities/platform/api/platform.service';

describe('PlatformTenantTableComponent', () => {
  let component: PlatformTenantTableComponent;
  let fixture: ComponentFixture<PlatformTenantTableComponent>;

  const sampleTenants: PlatformTenant[] = [
    {
      id: 'tenant-101',
      name: 'Banco Andino',
      slug: 'banco-andino',
      schemaName: 'tenant_banco_andino',
      status: 'ACTIVE',
      planId: 'plan-corp',
      planCode: 'ENTERPRISE',
      planName: 'Plan Corporativo',
      subscriptionStatus: 'ACTIVE',
      active: true,
      legalName: 'Banco Andino S.A.',
      taxIdentifier: '1029384756',
      contactEmail: 'central@bancoandino.bo',
      contactPhone: '+591-22110099',
      ownerName: 'Ignacio Suárez',
      ownerEmail: 'isuarez@bancoandino.bo',
      createdAt: '2026-03-01T08:00:00Z',
      updatedAt: '2026-03-01T08:00:00Z'
    },
    {
      id: 'tenant-102',
      name: 'Minería del Altiplano',
      slug: 'mineria-altiplano',
      schemaName: 'tenant_mineria_altiplano',
      status: 'INACTIVE',
      planId: 'plan-basic',
      planCode: 'BASIC',
      planName: 'Plan Básico',
      subscriptionStatus: 'SUSPENDED',
      active: false,
      legalName: 'Minería del Altiplano Ltda.',
      taxIdentifier: '5544332211',
      contactEmail: 'info@mineria.bo',
      contactPhone: '+591-28282828',
      ownerName: 'Pedro Valenzuela',
      ownerEmail: 'pvalenzuela@mineria.bo',
      createdAt: '2026-03-05T09:00:00Z',
      updatedAt: '2026-03-05T09:00:00Z'
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [PlatformTenantTableComponent],
      providers: [provideRouter([])]
    }).compileComponents();

    fixture = TestBed.createComponent(PlatformTenantTableComponent);
    component = fixture.componentInstance;
    component.tenants = sampleTenants;
    fixture.detectChanges();
  });

  it('debe crear el componente y renderizar la lista de organizaciones', () => {
    expect(component).toBeTruthy();
    const rows = fixture.nativeElement.querySelectorAll('tbody tr');
    expect(rows.length).toBe(2);
  });

  it('debe emitir el evento viewDetails al solicitar ver detalles', () => {
    spyOn(component.viewDetails, 'emit');
    component.viewDetails.emit('tenant-101');
    expect(component.viewDetails.emit).toHaveBeenCalledWith('tenant-101');
  });

  it('debe emitir el evento activate al pulsar activar', () => {
    spyOn(component.activate, 'emit');
    component.activate.emit('tenant-102');
    expect(component.activate.emit).toHaveBeenCalledWith('tenant-102');
  });

  it('debe emitir el evento deactivate al pulsar suspender/desactivar', () => {
    spyOn(component.deactivate, 'emit');
    component.deactivate.emit('tenant-101');
    expect(component.deactivate.emit).toHaveBeenCalledWith('tenant-101');
  });

  it('debe resolver el nombre del plan a través de planName, planCode o mapa', () => {
    expect(component.resolvePlanName(sampleTenants[0])).toBe('Plan Corporativo');

    const tenantWithoutName: PlatformTenant = {
      ...sampleTenants[0],
      planName: undefined,
      planCode: 'PREMIUM'
    };
    expect(component.resolvePlanName(tenantWithoutName)).toBe('PREMIUM');

    const map = new Map<string, string>();
    map.set('custom-id', 'Plan Custom Map');
    component.planNames = map;
    const tenantWithMapOnly: PlatformTenant = {
      ...sampleTenants[0],
      planName: undefined,
      planCode: undefined,
      planId: 'custom-id'
    };
    expect(component.resolvePlanName(tenantWithMapOnly)).toBe('Plan Custom Map');
  });
});

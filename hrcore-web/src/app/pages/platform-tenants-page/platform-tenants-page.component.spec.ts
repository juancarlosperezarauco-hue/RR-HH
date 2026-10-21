import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PlatformTenantsPageComponent } from './platform-tenants-page.component';
import { PlatformTenantListUseCase } from '../../features/platform/application/platform-tenant-list.usecase';
import { PlatformTenantCreateUseCase } from '../../features/platform/application/platform-tenant-create.usecase';
import { PlatformTenantActivateUseCase } from '../../features/platform/application/platform-tenant-activate.usecase';
import { PlatformTenantDeactivateUseCase } from '../../features/platform/application/platform-tenant-deactivate.usecase';
import { PlatformPlanListUseCase } from '../../features/platform/application/platform-plan-list.usecase';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PlatformTenant } from '../../entities/platform/api/platform.service';

describe('PlatformTenantsPageComponent - Administración de Organizaciones', () => {
  let component: PlatformTenantsPageComponent;
  let fixture: ComponentFixture<PlatformTenantsPageComponent>;

  let mockListUseCase: {
    status: any;
    tenants: any;
    page: any;
    totalElements: any;
    totalPages: any;
    currentPage: any;
    error: any;
    loadTenants: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockCreateUseCase: {
    status: any;
    error: any;
    createTenant: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockActivateUseCase: {
    status: any;
    error: any;
    activateTenant: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockDeactivateUseCase: {
    status: any;
    error: any;
    deactivateTenant: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockPlanUseCase: {
    plans: any;
    error: any;
    loadPlans: jasmine.Spy;
  };

  let mockToastService: {
    success: jasmine.Spy;
    error: jasmine.Spy;
    warning: jasmine.Spy;
    info: jasmine.Spy;
  };

  const sampleTenants: PlatformTenant[] = [
    {
      id: 'tenant-1',
      name: 'Tech Solutions Bolivia',
      slug: 'tech-solutions',
      schemaName: 'tenant_tech_solutions',
      status: 'ACTIVE',
      planId: 'plan-enterprise-id',
      planCode: 'ENTERPRISE',
      planName: 'Plan Empresarial',
      subscriptionStatus: 'ACTIVE',
      active: true,
      legalName: 'Tech Solutions Bolivia S.R.L.',
      taxIdentifier: '1020304050',
      contactEmail: 'contacto@techsolutions.bo',
      contactPhone: '+591-22446688',
      ownerName: 'Carlos Mendoza',
      ownerEmail: 'carlos@techsolutions.bo',
      createdAt: '2026-01-10T10:00:00Z',
      updatedAt: '2026-01-10T10:00:00Z'
    },
    {
      id: 'tenant-2',
      name: 'Constructora del Sur',
      slug: 'constructora-sur',
      schemaName: 'tenant_constructora_sur',
      status: 'INACTIVE',
      planId: 'plan-basic-id',
      planCode: 'BASIC',
      planName: 'Plan Básico',
      subscriptionStatus: 'CANCELLED',
      active: false,
      legalName: 'Constructora del Sur S.A.',
      taxIdentifier: '9080706050',
      contactEmail: 'admin@constructorasur.bo',
      contactPhone: '+591-44332211',
      ownerName: 'Lucía Fernández',
      ownerEmail: 'lucia@constructorasur.bo',
      createdAt: '2026-02-15T14:30:00Z',
      updatedAt: '2026-02-15T14:30:00Z'
    }
  ];

  beforeEach(async () => {
    mockListUseCase = {
      status: signal('idle'),
      tenants: signal(sampleTenants),
      page: signal({
        content: sampleTenants,
        totalElements: 2,
        totalPages: 1,
        size: 50,
        number: 0
      }),
      totalElements: signal(2),
      totalPages: signal(1),
      currentPage: signal(0),
      error: signal<string | null>(null),
      loadTenants: jasmine.createSpy('loadTenants').and.returnValue(Promise.resolve()),
      resetState: jasmine.createSpy('resetState')
    };

    mockCreateUseCase = {
      status: signal('idle'),
      error: signal<string | null>(null),
      createTenant: jasmine.createSpy('createTenant').and.returnValue(Promise.resolve(true)),
      resetState: jasmine.createSpy('resetState')
    };

    mockActivateUseCase = {
      status: signal('idle'),
      error: signal<string | null>(null),
      activateTenant: jasmine.createSpy('activateTenant').and.returnValue(Promise.resolve(true)),
      resetState: jasmine.createSpy('resetState')
    };

    mockDeactivateUseCase = {
      status: signal('idle'),
      error: signal<string | null>(null),
      deactivateTenant: jasmine.createSpy('deactivateTenant').and.returnValue(Promise.resolve(true)),
      resetState: jasmine.createSpy('resetState')
    };

    mockPlanUseCase = {
      plans: signal([
        {
          id: 'plan-enterprise-id',
          code: 'ENTERPRISE',
          name: 'Plan Empresarial',
          description: 'Para grandes empresas',
          maxUsers: 500,
          maxRoles: 50,
          planType: 'PAID',
          active: true
        },
        {
          id: 'plan-basic-id',
          code: 'BASIC',
          name: 'Plan Básico',
          description: 'Para pequeñas empresas',
          maxUsers: 10,
          maxRoles: 2,
          planType: 'FREE',
          active: true
        }
      ]),
      error: signal<string | null>(null),
      loadPlans: jasmine.createSpy('loadPlans').and.returnValue(Promise.resolve())
    };

    mockToastService = {
      success: jasmine.createSpy('success'),
      error: jasmine.createSpy('error'),
      warning: jasmine.createSpy('warning'),
      info: jasmine.createSpy('info')
    };

    await TestBed.configureTestingModule({
      imports: [PlatformTenantsPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideRouter([]),
        { provide: PlatformTenantListUseCase, useValue: mockListUseCase },
        { provide: PlatformTenantCreateUseCase, useValue: mockCreateUseCase },
        { provide: PlatformTenantActivateUseCase, useValue: mockActivateUseCase },
        { provide: PlatformTenantDeactivateUseCase, useValue: mockDeactivateUseCase },
        { provide: PlatformPlanListUseCase, useValue: mockPlanUseCase },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(PlatformTenantsPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('debe inicializar y cargar la lista de organizaciones y planes', () => {
    expect(component).toBeTruthy();
    expect(mockListUseCase.loadTenants).toHaveBeenCalledWith(0, 50);
    expect(mockPlanUseCase.loadPlans).toHaveBeenCalledWith(0, 200);
  });

  it('debe calcular las métricas y estadísticas correctamente (total, activas, inactivas)', () => {
    const stats = component.tenantStats();
    expect(stats.total).toBe(2);
    expect(stats.active).toBe(1);
    expect(stats.inactive).toBe(1);
  });

  describe('Criterio 2: Filtrado y búsqueda avanzada', () => {
    it('debe filtrar organizaciones por texto en nombre comercial o razón social', () => {
      component.setSearchQuery('Tech');
      const filtered = component.filteredTenants();
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Tech Solutions Bolivia');
      expect(component.hasActiveFilters()).toBeTrue();
    });

    it('debe filtrar organizaciones por identificación fiscal / NIT', () => {
      component.setNitQuery('9080706050');
      const filtered = component.filteredTenants();
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Constructora del Sur');
      expect(component.hasActiveFilters()).toBeTrue();
    });

    it('debe filtrar organizaciones por plan contratado', () => {
      component.setPlanFilter('ENTERPRISE');
      const filtered = component.filteredTenants();
      expect(filtered.length).toBe(1);
      expect(filtered[0].planCode).toBe('ENTERPRISE');
    });

    it('debe filtrar organizaciones por estado operativo (activo/inactivo)', () => {
      component.setStatusFilter('INACTIVE');
      const filtered = component.filteredTenants();
      expect(filtered.length).toBe(1);
      expect(filtered[0].active).toBeFalse();
      expect(filtered[0].status).toBe('INACTIVE');
    });

    it('debe restablecer todos los filtros al invocar clearFilters()', () => {
      component.setSearchQuery('Filtro temporal');
      component.setNitQuery('123456');
      component.setPlanFilter('ENTERPRISE');
      component.setStatusFilter('ACTIVE');
      expect(component.hasActiveFilters()).toBeTrue();

      component.clearFilters();

      expect(component.searchQuery()).toBe('');
      expect(component.nitQuery()).toBe('');
      expect(component.planFilter()).toBe('all');
      expect(component.statusFilter()).toBe('all');
      expect(component.hasActiveFilters()).toBeFalse();
      expect(component.filteredTenants().length).toBe(2);
    });
  });

  describe('Criterio 3: Visualización del detalle de la organización', () => {
    it('debe abrir el modal con el detalle completo de la organización seleccionada', () => {
      component.viewDetails('tenant-1');
      expect(component.selectedTenant()).not.toBeNull();
      expect(component.selectedTenant()?.id).toBe('tenant-1');
      expect(component.selectedTenant()?.ownerName).toBe('Carlos Mendoza');
      expect(component.selectedTenant()?.legalName).toBe('Tech Solutions Bolivia S.R.L.');
      expect(component.selectedTenant()?.taxIdentifier).toBe('1020304050');
      expect(component.selectedTenant()?.contactEmail).toBe('contacto@techsolutions.bo');
    });

    it('debe cerrar el modal al llamar a closeModal()', () => {
      component.viewDetails('tenant-1');
      expect(component.selectedTenant()).not.toBeNull();

      component.closeModal();
      expect(component.selectedTenant()).toBeNull();
    });
  });

  describe('Criterio 4: Activación y desactivación del estado operativo', () => {
    it('debe activar una organización tras confirmación del usuario y mostrar toast', async () => {
      spyOn(window, 'confirm').and.returnValue(true);

      await component.onActivate('tenant-2');

      expect(mockActivateUseCase.activateTenant).toHaveBeenCalledWith('tenant-2');
      expect(mockToastService.success).toHaveBeenCalledWith(
        jasmine.stringMatching(/activada exitosamente/i)
      );
      expect(mockListUseCase.loadTenants).toHaveBeenCalled();
    });

    it('no debe activar la organización si el usuario cancela la confirmación', async () => {
      spyOn(window, 'confirm').and.returnValue(false);

      await component.onActivate('tenant-2');

      expect(mockActivateUseCase.activateTenant).not.toHaveBeenCalled();
      expect(mockToastService.success).not.toHaveBeenCalled();
    });

    it('debe suspender/desactivar una organización tras confirmación del usuario y mostrar toast', async () => {
      spyOn(window, 'confirm').and.returnValue(true);

      await component.onDeactivate('tenant-1');

      expect(mockDeactivateUseCase.deactivateTenant).toHaveBeenCalledWith('tenant-1');
      expect(mockToastService.success).toHaveBeenCalledWith(
        jasmine.stringMatching(/desactivada exitosamente/i)
      );
      expect(mockListUseCase.loadTenants).toHaveBeenCalled();
    });

    it('no debe desactivar la organización si el usuario cancela la confirmación', async () => {
      spyOn(window, 'confirm').and.returnValue(false);

      await component.onDeactivate('tenant-1');

      expect(mockDeactivateUseCase.deactivateTenant).not.toHaveBeenCalled();
      expect(mockToastService.success).not.toHaveBeenCalled();
    });
  });
});

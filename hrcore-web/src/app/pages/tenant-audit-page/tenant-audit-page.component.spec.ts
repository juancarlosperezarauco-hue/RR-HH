import { ComponentFixture, TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { TenantAuditPageComponent } from './tenant-audit-page.component';
import { TenantAuditService, TenantAuditEventResponse } from '../../entities/audit';
import { ApiResponse, PageResponse } from '../../shared/api';

describe('TenantAuditPageComponent', () => {
  let component: TenantAuditPageComponent;
  let fixture: ComponentFixture<TenantAuditPageComponent>;
  let auditServiceSpy: jasmine.SpyObj<TenantAuditService>;

  const mockEvent1: TenantAuditEventResponse = {
    id: 'evt-001',
    actorId: 'usr-100',
    actorEmail: 'admin@tenant.com',
    actorSubject: 'Administrador Principal',
    tenantSlug: 'tenant-acme',
    eventType: 'USER_CREATED',
    resourceType: 'USER',
    resourceId: 'usr-200',
    eventDetails: '{"operation":"CREATE_USER","email":"empleado@tenant.com"}',
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    requestId: 'req-12345',
    correlationId: 'corr-67890',
    source: 'APPLICATION',
    outcome: 'SUCCESS',
    beforeState: null,
    afterState: '{"id":"usr-200","active":true}',
    createdAt: '2026-10-20T01:00:00Z'
  };

  const mockEvent2: TenantAuditEventResponse = {
    id: 'evt-002',
    actorId: 'usr-100',
    actorEmail: 'admin@tenant.com',
    actorSubject: 'Administrador Principal',
    tenantSlug: 'tenant-acme',
    eventType: 'USER_ROLES_ASSIGNED',
    resourceType: 'USER',
    resourceId: 'usr-200',
    eventDetails: '{"operation":"ASSIGN_USER_ROLES","roleCount":2}',
    ipAddress: '192.168.1.100',
    userAgent: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64)',
    requestId: 'req-12346',
    correlationId: 'corr-67891',
    source: 'APPLICATION',
    outcome: 'SUCCESS',
    beforeState: '{"roleCodes":["ROL-00001"]}',
    afterState: '{"roleCodes":["ROL-00001","ROL-00002"]}',
    createdAt: '2026-10-20T01:05:00Z'
  };

  const mockPageResponse: ApiResponse<PageResponse<TenantAuditEventResponse>> = {
    success: true,
    message: 'Audit events retrieved successfully',
    data: {
      content: [mockEvent1, mockEvent2],
      totalElements: 2,
      totalPages: 1,
      size: 20,
      page: 0,
      first: true,
      last: true,
      empty: false
    }
  };

  beforeEach(async () => {
    auditServiceSpy = jasmine.createSpyObj('TenantAuditService', ['getAuditEvents', 'getAuditEventById']);
    auditServiceSpy.getAuditEvents.and.returnValue(of(mockPageResponse));

    await TestBed.configureTestingModule({
      imports: [TenantAuditPageComponent],
      providers: [
        { provide: TenantAuditService, useValue: auditServiceSpy }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(TenantAuditPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('Criterio 1 y 2: debe crearse y cargar la lista paginada de eventos cronológicamente', () => {
    expect(component).toBeTruthy();
    expect(auditServiceSpy.getAuditEvents).toHaveBeenCalledWith(jasmine.any(Object), 0, 20);
    expect(component.events().length).toBe(2);
    expect(component.totalElements()).toBe(2);
    expect(component.events()[0].id).toBe('evt-001');
  });

  it('Criterio 4: debe abrir el modal de detalle con IP, User Agent, y metadatos del cambio', () => {
    expect(component.selectedEvent()).toBeNull();

    component.openDetailModal(mockEvent1);
    expect(component.selectedEvent()).toEqual(mockEvent1);
    expect(component.selectedEvent()?.ipAddress).toBe('192.168.1.100');
    expect(component.selectedEvent()?.userAgent).toContain('Mozilla/5.0');
    expect(component.selectedEvent()?.eventDetails).toContain('CREATE_USER');

    component.closeDetailModal();
    expect(component.selectedEvent()).toBeNull();
  });

  it('Criterio 3: debe permitir filtrar por rango de fechas, usuario ejecutor, módulo y recurso', async () => {
    component.startDateFilter.set('2026-10-01');
    component.endDateFilter.set('2026-10-20');
    component.actorFilter.set('admin@tenant.com');
    component.moduleFilter.set('USER');
    component.resourceTypeFilter.set('USER');
    component.actionFilter.set('USER_CREATED');
    component.searchFilter.set('CREATE_USER');

    await component.reload(0);

    expect(auditServiceSpy.getAuditEvents).toHaveBeenCalledWith(
      jasmine.objectContaining({
        actor: 'admin@tenant.com',
        module: 'USER',
        action: 'USER_CREATED',
        resourceType: 'USER',
        search: 'CREATE_USER'
      }),
      0,
      20
    );
  });

  it('Criterio 3: debe restablecer todos los filtros al llamar resetFilters', () => {
    component.startDateFilter.set('2026-10-01');
    component.actorFilter.set('admin@tenant.com');
    component.moduleFilter.set('USER');

    component.resetFilters();

    expect(component.startDateFilter()).toBe('');
    expect(component.actorFilter()).toBe('');
    expect(component.moduleFilter()).toBe('all');
    expect(component.actionFilter()).toBe('all');
    expect(component.resourceTypeFilter()).toBe('all');
    expect(component.searchFilter()).toBe('');
  });

  it('Criterio 2: debe manejar cambios de página', () => {
    component.onPageChange(2);
    expect(auditServiceSpy.getAuditEvents).toHaveBeenCalledWith(jasmine.any(Object), 2, 20);
  });

  it('debe manejar error del servidor limpiando la lista de eventos', async () => {
    auditServiceSpy.getAuditEvents.and.returnValue(throwError(() => new Error('Error al cargar')));

    await component.reload(0);

    expect(component.events()).toEqual([]);
    expect(component.page()).toBeNull();
    expect(component.isLoading()).toBeFalse();
  });
});

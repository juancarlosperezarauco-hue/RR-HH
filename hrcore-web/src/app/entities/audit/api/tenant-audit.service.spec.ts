import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TenantAuditService } from './tenant-audit.service';
import { environment } from '../../../../environments/environment';

describe('TenantAuditService', () => {
  let service: TenantAuditService;
  let httpMock: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        TenantAuditService,
        provideHttpClient(),
        provideHttpClientTesting()
      ]
    });

    service = TestBed.inject(TenantAuditService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('debe solicitar eventos de auditoría con parámetros de paginación y filtros', () => {
    service.getAuditEvents(
      {
        startDate: '2026-10-01T00:00:00Z',
        endDate: '2026-10-20T23:59:59Z',
        actor: 'admin@tenant.com',
        module: 'USER',
        action: 'CREATE',
        resourceType: 'USER',
        search: 'empleado'
      },
      1,
      25
    ).subscribe();

    const req = httpMock.expectOne(request => {
      return request.url === `${environment.apiUrl}/api/audit/events` &&
        request.params.get('page') === '1' &&
        request.params.get('size') === '25' &&
        request.params.get('actor') === 'admin@tenant.com' &&
        request.params.get('module') === 'USER' &&
        request.params.get('action') === 'CREATE' &&
        request.params.get('resourceType') === 'USER' &&
        request.params.get('search') === 'empleado';
    });

    expect(req.request.method).toBe('GET');
    req.flush({ success: true, message: 'OK', data: { content: [], totalElements: 0, totalPages: 0, size: 25, page: 1 } });
  });

  it('debe consultar el detalle de un evento por ID', () => {
    const eventId = '123e4567-e89b-12d3-a456-426614174000';

    service.getAuditEventById(eventId).subscribe(response => {
      expect(response.success).toBeTrue();
      expect(response.data?.id).toBe(eventId);
    });

    const req = httpMock.expectOne(`${environment.apiUrl}/api/audit/events/${eventId}`);
    expect(req.request.method).toBe('GET');
    req.flush({
      success: true,
      message: 'OK',
      data: {
        id: eventId,
        eventType: 'USER_CREATED',
        resourceType: 'USER',
        resourceId: 'usr-1',
        createdAt: '2026-10-20T00:00:00Z'
      }
    });
  });
});

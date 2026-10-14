import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { DepartmentsService } from './departments.service';

describe('DepartmentsService: contrato backend HU11', () => {
  let service: DepartmentsService;
  let http: HttpTestingController;
  const api = `${environment.apiUrl}/api/departments`;
  const reply = (data: unknown) => ({ success: true, message: 'OK', data, timestamp: '' });
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); service = TestBed.inject(DepartmentsService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('envía filtros reales, página cero y orden permitido', () => {
    service.list(0, 25, { name: ' Finanzas ', code: 'FIN', status: 'ACTIVE', parentDepartmentId: 'parent' }, 'updatedAt,desc').subscribe();
    const req = http.expectOne(r => r.url === api);
    expect(req.request.method).toBe('GET');
    expect(req.request.params.get('name')).toBe('Finanzas');
    expect(req.request.params.get('code')).toBe('FIN');
    expect(req.request.params.get('parentDepartmentId')).toBe('parent');
    expect(req.request.params.get('page')).toBe('0');
    expect(req.request.params.get('sort')).toBe('updatedAt,desc');
    expect(req.request.params.has('search')).toBeFalse(); req.flush(reply({ content: [] }));
  });
  it('envía reason en la ruta específica de desactivación', () => {
    service.changeStatus('department-id', false, 'Reorganización').subscribe();
    const req = http.expectOne(`${api}/department-id/deactivate`);
    expect(req.request.method).toBe('PATCH'); expect(req.request.body).toEqual({ reason: 'Reorganización' }); req.flush(reply({}));
  });
  it('interpreta historial paginado y no envía orden id rechazado por checkedPage', () => {
    service.history('department-id', 2).subscribe(r => expect(r.data.content.length).toBe(0));
    const req = http.expectOne(r => r.url === `${api}/department-id/history`);
    expect(req.request.params.get('page')).toBe('2'); expect(req.request.params.has('sort')).toBeFalse(); req.flush(reply({ content: [] }));
  });
  it('rechaza success=false aunque HTTP sea 200', () => {
    service.changeStatus('department-id', true, null).subscribe({ next: () => fail('No debe informar éxito'), error: e => expect(e.message).toBe('Plan bloqueado') });
    http.expectOne(`${api}/department-id/activate`).flush({ success: false, message: 'Plan bloqueado', data: null });
  });
});

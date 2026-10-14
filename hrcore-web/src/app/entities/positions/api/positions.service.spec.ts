import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { environment } from '../../../../environments/environment';
import { PositionsService } from './positions.service';
import { PositionRequest } from '../model/positions.model';

describe('PositionsService: contrato backend HU12', () => {
  let service: PositionsService; let http: HttpTestingController;
  const api = `${environment.apiUrl}/api/positions`;
  const reply = (data: unknown) => ({ success: true, message: 'OK', data, timestamp: '' });
  beforeEach(() => { TestBed.configureTestingModule({ providers: [provideHttpClient(), provideHttpClientTesting()] }); service = TestBed.inject(PositionsService); http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  it('usa organizationalLevel y no inventa departmentId', () => {
    service.list(1, 50, { organizationalLevel: 'MANAGEMENT', status: 'INACTIVE', name: ' Jefe ' }).subscribe();
    const req = http.expectOne(r => r.url === api);
    expect(req.request.params.get('organizationalLevel')).toBe('MANAGEMENT'); expect(req.request.params.get('name')).toBe('Jefe'); expect(req.request.params.has('departmentId')).toBeFalse(); req.flush(JSON.stringify(reply({ content: [] })));
  });
  it('activa sin un DTO de estado o motivo de departamentos', () => {
    service.changeStatus('position-id', true).subscribe();
    const req = http.expectOne(`${api}/position-id/activate`);
    expect(req.request.method).toBe('PATCH'); expect(req.request.body).toBeNull(); req.flush(JSON.stringify(reply({})));
  });
  it('crea y modifica requisitos con mandatory=false y cero años', () => {
    const body = { requirementType: 'EXPERIENCE' as const, description: 'Experiencia', mandatory: false, minimumYearsExperience: 0 };
    service.createRequirement('position-id', body).subscribe();
    const create = http.expectOne(`${api}/position-id/requirements`); expect(create.request.method).toBe('POST'); expect(create.request.body).toEqual(body); create.flush(reply({}));
    service.updateRequirement('position-id', 'requirement-id', body).subscribe();
    const update = http.expectOne(`${api}/position-id/requirements/requirement-id`); expect(update.request.method).toBe('PUT'); expect(update.request.body).toEqual(body); update.flush(reply({}));
  });
  it('elimina requisito con data null y mantiene posición en la ruta', () => {
    service.deleteRequirement('position-id', 'requirement-id').subscribe(r => expect(r.data).toBeNull());
    const req = http.expectOne(`${api}/position-id/requirements/requirement-id`); expect(req.request.method).toBe('DELETE'); req.flush(reply(null));
  });
  it('pagina requisitos con el orden permitido createdAt', () => {
    service.requirements('position-id', 3).subscribe();
    const req = http.expectOne(r => r.url === `${api}/position-id/requirements`); expect(req.request.params.get('page')).toBe('3'); expect(req.request.params.get('sort')).toBe('createdAt,asc'); req.flush(reply({ content: [] }));
  });
  it('envía POST y PUT como application/json con números decimales exactos', () => {
    const fields = { code: 'DEV', name: 'Desarrollador', description: null, responsibilities: null, organizationalLevel: 'PROFESSIONAL' as const, currency: 'BOB' };
    const body: PositionRequest = { ...fields, salaryMin: '12345678901234.1234', salaryMax: '999999999999999.9999' };
    const exactBody = `${JSON.stringify(fields).slice(0, -1)},"salaryMin":12345678901234.1234,"salaryMax":999999999999999.9999}`;
    const response = '{"success":true,"message":"OK","data":{"salaryMin":12345678901234.1234,"salaryMax":999999999999999.9999},"timestamp":""}';
    service.create(body).subscribe(r => expect(r.data.salaryMin).toBe(body.salaryMin));
    const create = http.expectOne(api);
    expect(create.request.method).toBe('POST'); expect(create.request.headers.get('Content-Type')).toBe('application/json');
    expect(create.request.serializeBody()).toBe(exactBody); create.flush(response);
    service.update('position-id', body).subscribe(r => expect(r.data.salaryMax).toBe(body.salaryMax));
    const update = http.expectOne(`${api}/position-id`);
    expect(update.request.method).toBe('PUT'); expect(update.request.serializeBody()).toBe(exactBody); update.flush(response);
  });
  it('conserva salarios al consultar detalle y listado', () => {
    const position = '{"salaryMin":12345678901234.1234,"salaryMax":null}';
    service.get('position-id').subscribe(r => expect(r.data.salaryMin).toBe('12345678901234.1234'));
    http.expectOne(`${api}/position-id`).flush(`{"success":true,"data":${position}}`);
    service.list().subscribe(r => { expect(r.data.content[0].salaryMin).toBe('12345678901234.1234'); expect(r.data.content[0].salaryMax).toBeNull(); });
    http.expectOne(r => r.url === api).flush(`{"success":true,"data":{"content":[${position}]}}`);
  });
  it('conserva mensajes y detalles JSON de los errores HTTP', () => {
    service.get('position-id').subscribe({ error: e => { expect(e.status).toBe(403); expect(e.error.message).toBe('Sin permiso'); expect(e.error.errors).toEqual(['Detalle']); } });
    http.expectOne(`${api}/position-id`).flush('{"message":"Sin permiso","errors":["Detalle"]}', { status: 403, statusText: 'Forbidden' });
  });
  it('conserva errores no JSON y success=false', () => {
    service.get('position-id').subscribe({ error: e => { expect(e.status).toBe(500); expect(e.error).toBe('Error externo'); } });
    http.expectOne(`${api}/position-id`).flush('Error externo', { status: 500, statusText: 'Server Error' });
    service.list().subscribe({ error: e => expect(e.message).toBe('Operación rechazada') });
    http.expectOne(r => r.url === api).flush('{"success":false,"message":"Operación rechazada","data":null}');
  });
});

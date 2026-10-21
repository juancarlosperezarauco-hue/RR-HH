import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { fakeAsync, flushMicrotasks, TestBed } from '@angular/core/testing';
import { AiConversationService } from './ai-conversation.service';
import { authTokenInterceptor } from '../../../shared/api/interceptors/auth.interceptor';
import { AuthStorageService } from '../../../shared/lib/storage/auth-storage.service';
import { PlatformStorageService } from '../../../features/platform/lib/platform-storage.service';
import { SessionCoordinatorService } from '../../../shared/lib/auth/session-coordinator.service';
import { StoredAuthSession } from '../../../shared/lib/auth/auth-session.model';

describe('AiConversationService: contrato e interceptor', () => {
  let service: AiConversationService, http: HttpTestingController;
  const api = '/api/hr/ai/conversations';
  const reply = <T>(data: T) => ({ success: true, data, message: 'OK', timestamp: '' });
  let current: StoredAuthSession;
  let sessions: { isLoggingOut: () => boolean; refreshSingleFlight: jasmine.Spy; getSession: () => StoredAuthSession; clearSession: jasmine.Spy };
  beforeEach(() => {
    current = { scope: 'TENANT', tenantSlug: 'synthetic', accessToken: 'synthetic-access', refreshToken: 'synthetic-refresh', accessExpiresAt: 1, revision: 'r1', loginId: 'login1' };
    sessions = { isLoggingOut: () => false, refreshSingleFlight: jasmine.createSpy(), getSession: () => current, clearSession: jasmine.createSpy() };
    TestBed.configureTestingModule({ providers: [
      provideHttpClient(withInterceptors([authTokenInterceptor])), provideHttpClientTesting(),
      { provide: AuthStorageService, useValue: { getSession: () => current } },
      { provide: PlatformStorageService, useValue: { getSession: () => null } },
      { provide: SessionCoordinatorService, useValue: sessions }
    ] });
    service = TestBed.inject(AiConversationService); http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('descarga cada formato como binario con JWT y tenant, sin enviar filas o planes', () => {
    for (const format of ['csv', 'xlsx', 'pdf'] as const) {
      service.export('c', 'run', format).subscribe(response => expect(response.body).toBeInstanceOf(Blob));
      const req = http.expectOne(r => r.url === `${api}/c/reports/run/export`);
      expect(req.request.method).toBe('POST'); expect(req.request.body).toBeNull();
      expect(req.request.params.keys()).toEqual(['format']); expect(req.request.params.get('format')).toBe(format);
      expect(req.request.responseType).toBe('blob');
      expect(req.request.headers.get('Authorization')).toBe('Bearer synthetic-access');
      expect(req.request.headers.get('X-Tenant-Slug')).toBe('synthetic'); req.flush(new Blob(['synthetic']));
    }
  });
  it('recupera el mensaje JSON de un error binario de exportación', done => {
    service.export('c', 'run', 'pdf').subscribe({ next: () => fail('Debe rechazar'), error: error => {
      expect(error.status).toBe(413); expect(error.error.message).toBe('Aplica filtros'); done();
    } });
    http.expectOne(r => r.url.endsWith('/export')).flush(new Blob([JSON.stringify({ message: 'Aplica filtros' })],
      { type: 'application/json' }), { status: 413, statusText: 'Too Large' });
  });
  it('crea sin cuerpo ni tenant/owner manipulables y reutiliza los headers de sesión', () => {
    service.create().subscribe(); const req = http.expectOne(api);
    expect(req.request.method).toBe('POST'); expect(req.request.body).toBeNull();
    expect(req.request.headers.get('Authorization')).toBe('Bearer synthetic-access');
    expect(req.request.headers.get('X-Tenant-Slug')).toBe('synthetic'); req.flush(reply({ id: 'c' }));
  });
  it('envía page/size y adapta number de Spring sin modificar el modelo compartido', () => {
    service.list(2, 20).subscribe(response => expect(response.data.page).toBe(2));
    const req = http.expectOne(r => r.url === api);
    expect(req.request.params.get('page')).toBe('2'); expect(req.request.params.get('size')).toBe('20');
    req.flush(reply({ content: [], number: 2, size: 20, totalPages: 3, totalElements: 40 }));
  });
  it('lee conversación e historial y elimina en las rutas actuales', () => {
    service.get('c').subscribe(); http.expectOne(`${api}/c`).flush(reply({ id: 'c' }));
    service.messages('c', 1, 50).subscribe(r => expect(r.data.page).toBe(1));
    const history = http.expectOne(r => r.url === `${api}/c/messages`);
    expect(history.request.params.get('page')).toBe('1'); expect(history.request.params.get('size')).toBe('50');
    history.flush(reply({ content: [], number: 1, size: 50 }));
    service.delete('c').subscribe(); const deletion = http.expectOne(`${api}/c`);
    expect(deletion.request.method).toBe('DELETE'); deletion.flush(reply(null));
  });
  it('envía exactamente clientMessageId/message y acepta pending sin reinterpretarlo', () => {
    const body = { clientMessageId: 'synthetic-id', message: 'pregunta ficticia' };
    service.send('c', body).subscribe(r => expect(r.data.status).toBe('pending'));
    const req = http.expectOne(`${api}/c/messages`); expect(req.request.body).toEqual(body);
    req.flush(reply({ status: 'pending' }), { status: 202, statusText: 'Accepted' });
  });
  it('refresca por runId y página sin enviar una pregunta', () => {
    service.refresh('c', 'run', 1, 10).subscribe(); const req = http.expectOne(r => r.url === `${api}/c/reports/run/refresh`);
    expect(req.request.body).toBeNull(); expect(req.request.method).toBe('POST');
    expect(req.request.params.get('page')).toBe('1'); expect(req.request.params.get('size')).toBe('10'); req.flush(reply({}));
    service.refresh('c', 'run').subscribe(); const original = http.expectOne(`${api}/c/reports/run/refresh`);
    expect(original.request.params.keys()).toEqual([]); original.flush(reply({}));
  });
  it('rechaza success=false y conserva los errores HTTP del backend', () => {
    service.get('c').subscribe({ next: () => fail('No debe aceptar false'), error: e => expect(e.message).toBe('Bloqueado') });
    http.expectOne(`${api}/c`).flush({ success: false, message: 'Bloqueado', data: null });
    service.messages('c').subscribe({ error: e => expect(e.status).toBe(403) });
    http.expectOne(r => r.url.endsWith('/c/messages')).flush({ code: 'QUERY_NOT_ALLOWED' }, { status: 403, statusText: 'Forbidden' });
  });
  it('el retry 401 del interceptor conserva el ID lógico y el tenant', fakeAsync(() => {
    const body = { clientMessageId: 'same-logical-id', message: 'pregunta ficticia' };
    const updated = { ...current, accessToken: 'synthetic-new', revision: 'r2' };
    sessions.refreshSingleFlight.and.callFake(() => { current = updated; return Promise.resolve(updated); });
    service.send('c', body).subscribe();
    http.expectOne(`${api}/c/messages`).flush({}, { status: 401, statusText: 'Unauthorized' }); flushMicrotasks();
    const retried = http.expectOne(`${api}/c/messages`);
    expect(retried.request.body).toEqual(body); expect(retried.request.headers.get('Authorization')).toBe('Bearer synthetic-new');
    expect(retried.request.headers.get('X-Tenant-Slug')).toBe('synthetic');
    retried.flush(reply({ status: 'ok' })); expect(sessions.refreshSingleFlight).toHaveBeenCalledTimes(1);
  }));
});

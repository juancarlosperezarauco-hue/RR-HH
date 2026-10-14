import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { importProvidersFrom } from '@angular/core';
import { Briefcase, LucideAngularModule } from 'lucide-angular';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { PositionsPageComponent } from './positions-page.component';
import { environment } from '../../../environments/environment';

describe('PositionsPageComponent: validaciones backend', () => {
  let page: PositionsPageComponent; let http: HttpTestingController;
  let fixture: ComponentFixture<PositionsPageComponent>;
  const api = `${environment.apiUrl}/api/positions`;
  const position = { id: 'position-id', code: 'DEV', name: 'Desarrollador', description: null, responsibilities: null, organizationalLevel: 'PROFESSIONAL', salaryMin: null, salaryMax: null, currency: null, status: 'ACTIVE', createdAt: '', updatedAt: '' };
  const reply = (data: unknown) => JSON.stringify({ success: true, message: 'OK', data, timestamp: '' });
  const listed = (content: unknown[] = [], number = 0) => reply({ content, number, totalPages: 1, totalElements: content.length });
  beforeEach(() => { TestBed.configureTestingModule({ imports: [PositionsPageComponent], providers: [provideHttpClient(), provideHttpClientTesting(), importProvidersFrom(LucideAngularModule.pick({ Briefcase })), { provide: PermissionService, useValue: { hasPermission: () => true, hasAnyPermission: () => true } }, { provide: ToastService, useValue: { warning: () => {}, error: () => {}, success: () => {} } }] }); fixture = TestBed.createComponent(PositionsPageComponent); page = fixture.componentInstance; http = TestBed.inject(HttpTestingController); page.mode = 'create'; page.form.code = 'DEV'; page.form.name = 'Desarrollador'; });
  afterEach(() => http.verify());
  it('rechaza rango salarial invertido', () => { page.form.salaryMin = '200'; page.form.salaryMax = '100'; page.save(); expect(page.panelError()).toContain('mínimo'); http.expectNone(() => true); });
  it('rechaza negativos y más de cuatro decimales', () => { for (const value of ['-1', '1.12345']) { page.form.salaryMin = value; page.save(); expect(page.panelError()).toContain('cuatro decimales'); } http.expectNone(() => true); });
  it('rechaza moneda fuera del contrato', () => { page.form.currency = 'bob'; page.save(); expect(page.panelError()).toContain('mayúsculas'); http.expectNone(() => true); });
  it('rechaza experiencia fraccionaria sin HTTP', () => { page.selected = { ...page.form, id: 'position', status: 'ACTIVE', createdAt: '', updatedAt: '' }; page.requirementForm.description = 'Experiencia'; page.requirementForm.minimumYearsExperience = 1.5; page.saveRequirement(); expect(page.panelError()).toContain('entero'); http.expectNone(() => true); });
  it('compara el rango grande sin perder la última cifra decimal', () => {
    page.form.salaryMin = '12345678901234.1234'; page.form.salaryMax = '12345678901234.1233';
    page.save(); expect(page.panelError()).toContain('mínimo'); http.expectNone(() => true);
  });
  it('muestra los salarios completos en la tabla sin redondearlos', () => {
    page.mode = null; fixture.detectChanges();
    const row = JSON.stringify(position).replace('"salaryMin":null', '"salaryMin":12345678901234.1234').replace('"salaryMax":null', '"salaryMax":999999999999999.9999');
    http.expectOne(r => r.url === api && r.method === 'GET').flush(`{"success":true,"data":{"content":[${row}],"number":0,"totalPages":1,"totalElements":1}}`);
    fixture.detectChanges();
    const salaryCell = (fixture.nativeElement as HTMLElement).querySelectorAll('tbody tr td')[2];
    expect(salaryCell.textContent).toContain('12345678901234.1234');
    expect(salaryCell.textContent).toContain('999999999999999.9999');
  });
  it('conserva el texto decimal introducido en el formulario real', async () => {
    fixture.detectChanges();
    const initial = http.expectOne(r => r.url === api && r.method === 'GET');
    initial.flush(listed()); await fixture.whenStable();
    const input = (fixture.nativeElement as HTMLElement).querySelector<HTMLInputElement>('input[name="salaryMin"]')!;
    expect(input.type).toBe('text'); input.value = '12345678901234.1234'; input.dispatchEvent(new Event('input'));
    await fixture.whenStable();
    expect(page.form.salaryMin).toBe('12345678901234.1234');
    page.save(); const create = http.expectOne(r => r.url === api && r.method === 'POST');
    expect(create.request.serializeBody()).toContain('"salaryMin":12345678901234.1234'); create.flush(reply(position));
    http.expectOne(r => r.url === api && r.method === 'GET').flush(listed());
  });
  it('crea una sola vez y recarga cuando no hay listado pendiente', () => {
    page.form.salaryMin = '12345678901234.1234'; page.form.salaryMax = '999999999999999.9999';
    page.save(); page.save();
    const create = http.expectOne(r => r.url === api && r.method === 'POST');
    expect(create.request.serializeBody()).toContain('"salaryMin":12345678901234.1234,"salaryMax":999999999999999.9999');
    expect(page.saving()).toBeTrue(); create.flush(reply(position));
    const refresh = http.expectOne(r => r.url === api && r.method === 'GET');
    expect(refresh.request.params.get('page')).toBe('0'); refresh.flush(listed([position]));
    expect(page.items()[0].id).toBe(position.id); expect(page.loading()).toBeFalse(); expect(page.saving()).toBeFalse();
  });
  it('reemplaza el listado pendiente al crear y conserva filtros, tamaño y orden', () => {
    page.filters = { name: ' Otro ', code: ' OTHER ', status: 'INACTIVE', organizationalLevel: 'MANAGEMENT' }; page.size = 50; page.sort = 'code,asc';
    page.load(3); const older = http.expectOne(r => r.url === api && r.method === 'GET');
    page.load(3); http.expectNone(r => r.method === 'GET');
    page.save(); page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush(reply(position));
    expect(older.cancelled).toBeTrue();
    const refresh = http.expectOne(r => r.url === api && r.method === 'GET');
    for (const [key, value] of Object.entries({ page: '0', size: '50', sort: 'code,asc', name: 'Otro', code: 'OTHER', status: 'INACTIVE', organizationalLevel: 'MANAGEMENT' })) expect(refresh.request.params.get(key)).toBe(value);
    refresh.flush(listed());
    expect(page.items()).toEqual([]); expect(page.page).toBe(0);
    expect(() => older.flush(listed([{ ...position, id: 'stale' }], 3))).toThrow();
    expect(page.items()).toEqual([]); expect(page.loading()).toBeFalse();
  });
  it('recarga también si el listado inicial termina antes de la creación', () => {
    page.load(2); const initial = http.expectOne(r => r.url === api && r.method === 'GET');
    page.save(); const create = http.expectOne(r => r.url === api && r.method === 'POST');
    initial.flush(listed([], 2)); create.flush(reply(position));
    http.expectOne(r => r.url === api && r.method === 'GET').flush(listed([position]));
    expect(page.items()[0].id).toBe(position.id); expect(page.page).toBe(0);
  });
  it('mantiene la consulta pendiente y los errores cuando falla la creación', () => {
    page.load(); const initial = http.expectOne(r => r.url === api && r.method === 'GET');
    page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush('{"message":"Código duplicado"}', { status: 400, statusText: 'Bad Request' });
    expect(initial.cancelled).toBeFalse(); expect(page.panelError()).toContain('Código duplicado'); expect(page.saving()).toBeFalse();
    http.expectNone(r => r.method === 'GET'); initial.flush(listed()); expect(page.loading()).toBeFalse();
  });
  it('libera loading y conserva el mensaje si falla la recarga posterior', () => {
    page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush(reply(position));
    http.expectOne(r => r.url === api && r.method === 'GET').flush('{"message":"Error de listado"}', { status: 500, statusText: 'Server Error' });
    expect(page.loading()).toBeFalse(); expect(page.saving()).toBeFalse(); expect(page.error()).toContain('Error de listado');
  });
});

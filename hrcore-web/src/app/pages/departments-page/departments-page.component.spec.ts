import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { DepartmentsService } from '../../entities/departments';
import { DepartmentsPageComponent } from './departments-page.component';
import { environment } from '../../../environments/environment';

describe('DepartmentsPageComponent: reglas de jerarquía', () => {
  let page: DepartmentsPageComponent; let http: HttpTestingController;
  const api = `${environment.apiUrl}/api/departments`;
  const reply = (data: unknown) => ({ success: true, message: 'OK', data, timestamp: '' });
  const listed = (content: unknown[] = [], number = 0) => reply({ content, number, totalPages: 1, totalElements: content.length });
  const prepareCreate = () => { page.mode = 'create'; page.form.code = 'FIN'; page.form.name = 'Finanzas'; };
  beforeEach(() => { TestBed.configureTestingModule({ imports: [DepartmentsPageComponent], providers: [provideHttpClient(), provideHttpClientTesting(), { provide: PermissionService, useValue: { hasPermission: () => true } }, { provide: ToastService, useValue: { warning: () => {}, error: () => {}, success: () => {} } }] }); page = TestBed.createComponent(DepartmentsPageComponent).componentInstance; http = TestBed.inject(HttpTestingController); });
  afterEach(() => http.verify());
  const department = { id: '11111111-1111-1111-1111-111111111111', code: 'FIN', name: 'Finanzas', status: 'ACTIVE' as const, description: null, parentDepartmentId: null, managerEmployeeId: null, costCenterCode: null, createdBy: '', updatedBy: '', createdAt: '', updatedAt: '' };
  it('rechaza padre propio y descendiente conocido', () => {
    page.selected = department;
    const child = { ...department, id: '22222222-2222-2222-2222-222222222222', parentDepartmentId: department.id };
    page.parents.set([department, child]); expect(page.validParent(department)).toBeFalse(); expect(page.validParent(child)).toBeFalse();
  });
  it('exige motivo al cambiar el superior y no envía HTTP inválido', () => {
    page.mode = 'edit'; page.selected = department;
    page.form = { code: 'FIN', name: 'Finanzas', description: null, parentDepartmentId: '22222222-2222-2222-2222-222222222222', managerEmployeeId: null, costCenterCode: null, reason: ' ' };
    page.save(); expect(page.panelError()).toContain('motivo'); http.expectNone(() => true);
  });
  it('bloquea doble envío mientras saving está activo', () => { const create = spyOn(TestBed.inject(DepartmentsService), 'create'); page.saving.set(true); page.mode = 'create'; page.form.code = 'FIN'; page.form.name = 'Finanzas'; page.save(); expect(create).not.toHaveBeenCalled(); http.expectNone(() => true); });
  it('crea una sola vez y recarga cuando no hay listado pendiente', () => {
    prepareCreate(); page.save(); page.save();
    const create = http.expectOne(r => r.url === api && r.method === 'POST');
    expect(page.saving()).toBeTrue(); create.flush(reply(department));
    const refresh = http.expectOne(r => r.url === api && r.method === 'GET');
    expect(refresh.request.params.get('page')).toBe('0'); refresh.flush(listed([department]));
    expect(page.items()[0].id).toBe(department.id); expect(page.loading()).toBeFalse(); expect(page.saving()).toBeFalse();
  });
  it('reemplaza el listado pendiente al crear y conserva filtros, tamaño y orden', () => {
    page.filters = { name: ' Otro ', code: ' OTHER ', status: 'INACTIVE', parentDepartmentId: '22222222-2222-2222-2222-222222222222' }; page.size = 50; page.sort = 'code,asc';
    page.load(3); const older = http.expectOne(r => r.url === api && r.method === 'GET');
    page.load(3); http.expectNone(r => r.method === 'GET');
    prepareCreate(); page.save(); page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush(reply(department));
    expect(older.cancelled).toBeTrue();
    const refresh = http.expectOne(r => r.url === api && r.method === 'GET');
    for (const [key, value] of Object.entries({ page: '0', size: '50', sort: 'code,asc', name: 'Otro', code: 'OTHER', status: 'INACTIVE', parentDepartmentId: page.filters.parentDepartmentId })) expect(refresh.request.params.get(key)).toBe(value);
    refresh.flush(listed());
    expect(page.items()).toEqual([]); expect(page.page).toBe(0);
    expect(() => older.flush(listed([{ ...department, id: 'stale' }], 3))).toThrow();
    expect(page.items()).toEqual([]); expect(page.loading()).toBeFalse();
  });
  it('recarga también si el listado inicial termina antes de la creación', () => {
    page.load(2); const initial = http.expectOne(r => r.url === api && r.method === 'GET');
    prepareCreate(); page.save(); const create = http.expectOne(r => r.url === api && r.method === 'POST');
    initial.flush(listed([], 2)); create.flush(reply(department));
    http.expectOne(r => r.url === api && r.method === 'GET').flush(listed([department]));
    expect(page.items()[0].id).toBe(department.id); expect(page.page).toBe(0);
  });
  it('mantiene la consulta pendiente y los errores cuando falla la creación', () => {
    page.load(); const initial = http.expectOne(r => r.url === api && r.method === 'GET');
    prepareCreate(); page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush({ message: 'Código duplicado' }, { status: 400, statusText: 'Bad Request' });
    expect(initial.cancelled).toBeFalse(); expect(page.panelError()).toContain('Código duplicado'); expect(page.saving()).toBeFalse();
    http.expectNone(r => r.method === 'GET'); initial.flush(listed()); expect(page.loading()).toBeFalse();
  });
  it('libera loading y conserva el mensaje si falla la recarga posterior', () => {
    prepareCreate(); page.save(); http.expectOne(r => r.url === api && r.method === 'POST').flush(reply(department));
    http.expectOne(r => r.url === api && r.method === 'GET').flush({ message: 'Error de listado' }, { status: 500, statusText: 'Server Error' });
    expect(page.loading()).toBeFalse(); expect(page.saving()).toBeFalse(); expect(page.error()).toContain('Error de listado');
  });
});

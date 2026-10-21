import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { UserRoleUseCase } from './user-role.usecase';
import { AccessService, TenantRoleResponse, UserRolesResponse } from '../../../entities/access';
import { ApiResponse } from '../../../shared/api';
import { PageResponse } from '../../../entities/access/model/page-response.model';

describe('UserRoleUseCase', () => {
  let useCase: UserRoleUseCase;
  let accessServiceSpy: jasmine.SpyObj<AccessService>;

  const mockUserRole: TenantRoleResponse = {
    id: 'role-1',
    code: 'ROL-00001',
    name: 'Supervisor',
    description: 'Rol operativo',
    roleType: 'INTERNAL',
    systemRole: false,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    permissionCodes: ['employees.list']
  };

  const mockAvailableRole: TenantRoleResponse = {
    id: 'role-2',
    code: 'ROL-00002',
    name: 'Analista',
    description: 'Rol analítico',
    roleType: 'INTERNAL',
    systemRole: false,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    permissionCodes: ['reports.read']
  };

  beforeEach(() => {
    accessServiceSpy = jasmine.createSpyObj('AccessService', ['getUserRoles', 'getRoles', 'assignUserRoles']);

    TestBed.configureTestingModule({
      providers: [
        UserRoleUseCase,
        { provide: AccessService, useValue: accessServiceSpy }
      ]
    });

    useCase = TestBed.inject(UserRoleUseCase);
  });

  it('debe crearse e iniciar en estado idle con listas vacías', () => {
    expect(useCase).toBeTruthy();
    expect(useCase.status()).toBe('idle');
    expect(useCase.userRoles()).toEqual([]);
    expect(useCase.availableRoles()).toEqual([]);
    expect(useCase.error()).toBeNull();
  });

  it('debe cargar los roles del usuario y los roles disponibles del tenant', async () => {
    const userRolesResponse: ApiResponse<UserRolesResponse> = {
      success: true,
      message: 'OK',
      data: {
        userId: 'user-123',
        roles: [mockUserRole]
      }
    };

    const rolesPageResponse: ApiResponse<PageResponse<TenantRoleResponse>> = {
      success: true,
      message: 'OK',
      data: {
        content: [mockUserRole, mockAvailableRole],
        totalElements: 2,
        totalPages: 1,
        size: 200,
        number: 0,
        first: true,
        last: true,
        empty: false
      }
    };

    accessServiceSpy.getUserRoles.and.returnValue(of(userRolesResponse));
    accessServiceSpy.getRoles.and.returnValue(of(rolesPageResponse));

    await useCase.loadRolesForUser('user-123');

    expect(useCase.status()).toBe('idle');
    expect(useCase.userRoles().length).toBe(1);
    expect(useCase.userRoles()[0].id).toBe('role-1');
    expect(useCase.availableRoles().length).toBe(2);
    expect(useCase.error()).toBeNull();
  });

  it('debe asignar roles al usuario y actualizar el estado a success', async () => {
    const assignResponse: ApiResponse<UserRolesResponse> = {
      success: true,
      message: 'Roles assigned',
      data: {
        userId: 'user-123',
        roles: [mockUserRole, mockAvailableRole]
      }
    };

    accessServiceSpy.assignUserRoles.and.returnValue(of(assignResponse));

    await useCase.assignRoles('user-123', ['role-1', 'role-2']);

    expect(useCase.status()).toBe('success');
    expect(useCase.userRoles().length).toBe(2);
    expect(useCase.error()).toBeNull();
    expect(accessServiceSpy.assignUserRoles).toHaveBeenCalledWith('user-123', {
      roleIds: ['role-1', 'role-2']
    });
  });

  it('debe manejar error cuando falla la asignación de roles', async () => {
    accessServiceSpy.assignUserRoles.and.returnValue(
      throwError(() => ({ error: { message: 'No se puede asignar un rol inactivo' } }))
    );

    await useCase.assignRoles('user-123', ['role-1']);

    expect(useCase.status()).toBe('error');
    expect(useCase.error()).toBe('No se puede asignar un rol inactivo');
  });

  it('debe reiniciar el estado a idle al llamar resetState', () => {
    useCase.resetState();
    expect(useCase.status()).toBe('idle');
    expect(useCase.userRoles()).toEqual([]);
    expect(useCase.availableRoles()).toEqual([]);
    expect(useCase.error()).toBeNull();
  });
});

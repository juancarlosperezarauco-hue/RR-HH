import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { of } from 'rxjs';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { UsersPageComponent } from './users-page.component';
import {
  UserListUseCase,
  UserCreateUseCase,
  UserUpdateUseCase,
  UserStatusUseCase
} from '../../features/user-management';
import { UserRoleUseCase } from '../../features/user-role-management';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { EmployeesService } from '../../entities/employees/api/employees.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { TenantUserResponse } from '../../entities/user';

describe('UsersPageComponent - Administración de Usuarios del Tenant', () => {
  let component: UsersPageComponent;
  let fixture: ComponentFixture<UsersPageComponent>;

  const mockUsers: TenantUserResponse[] = [
    {
      id: 'usr-1',
      email: 'admin@empresa.com',
      firstName: 'Admin',
      lastName: 'Principal',
      displayName: 'Admin Principal',
      active: true,
      status: 'ACTIVE',
      roles: [{ id: 'r-1', code: 'OWNER_ADMIN', name: 'Administrador', active: true }],
      employeeId: 'emp-1',
      employeeNumber: 'EMP-001',
      employeeFullName: 'Admin Principal Corp',
      createdAt: '2026-01-01T10:00:00Z',
      updatedAt: '2026-01-01T10:00:00Z'
    },
    {
      id: 'usr-2',
      email: 'empleado@empresa.com',
      firstName: 'Juan',
      lastName: 'Pérez',
      displayName: 'Juan Pérez Ventas',
      active: true,
      status: 'ACTIVE',
      roles: [{ id: 'r-2', code: 'ROL-00001', name: 'Empleado', active: true }],
      employeeId: 'emp-2',
      employeeNumber: 'EMP-002',
      employeeFullName: 'Juan Pérez Comercial',
      createdAt: '2026-02-01T10:00:00Z',
      updatedAt: '2026-02-01T10:00:00Z'
    },
    {
      id: 'usr-3',
      email: 'inactivo@empresa.com',
      firstName: 'Pedro',
      lastName: 'Gómez',
      displayName: 'Pedro Gómez',
      active: false,
      status: 'INACTIVE',
      roles: [{ id: 'r-2', code: 'ROL-00001', name: 'Empleado', active: true }],
      createdAt: '2026-03-01T10:00:00Z',
      updatedAt: '2026-03-01T10:00:00Z'
    },
    {
      id: 'usr-4',
      email: 'pendiente@empresa.com',
      firstName: 'Laura',
      lastName: 'Méndez',
      displayName: 'Laura Méndez',
      active: false,
      status: 'PENDING',
      roles: [{ id: 'r-2', code: 'ROL-00001', name: 'Empleado', active: true }],
      createdAt: '2026-04-01T10:00:00Z',
      updatedAt: '2026-04-01T10:00:00Z'
    }
  ];

  let mockListUseCase: {
    status: any;
    data: any;
    page: any;
    error: any;
    loadUsers: jasmine.Spy;
  };

  let mockCreateUseCase: {
    status: any;
    error: any;
    createUser: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockUpdateUseCase: {
    status: any;
    error: any;
    updateUser: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockStatusUseCase: {
    status: any;
    error: any;
    toggleStatus: jasmine.Spy;
  };

  let mockRoleUseCase: {
    status: any;
    error: any;
    availableRoles: any;
    userRoles: any;
    loadAvailableRoles: jasmine.Spy;
    loadRolesForUser: jasmine.Spy;
    assignRoles: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockPermissionService: {
    isCurrentOwner: jasmine.Spy;
    hasPermission: jasmine.Spy;
  };

  let mockAuthFacade: {
    currentUser: any;
  };

  let mockEmployeesService: {
    list: jasmine.Spy;
  };

  let mockToastService: {
    success: jasmine.Spy;
    error: jasmine.Spy;
    warning: jasmine.Spy;
    info: jasmine.Spy;
  };

  beforeEach(async () => {
    mockListUseCase = {
      status: signal('success'),
      data: signal(mockUsers),
      page: signal({
        content: mockUsers,
        totalElements: mockUsers.length,
        totalPages: 1,
        size: 20,
        number: 0,
        first: true,
        last: true,
        empty: false
      }),
      error: signal(null),
      loadUsers: jasmine.createSpy('loadUsers').and.resolveTo()
    };

    mockCreateUseCase = {
      status: signal('idle'),
      error: signal(null),
      createUser: jasmine.createSpy('createUser'),
      resetState: jasmine.createSpy('resetState')
    };

    mockUpdateUseCase = {
      status: signal('idle'),
      error: signal(null),
      updateUser: jasmine.createSpy('updateUser'),
      resetState: jasmine.createSpy('resetState')
    };

    mockStatusUseCase = {
      status: signal('idle'),
      error: signal(null),
      toggleStatus: jasmine.createSpy('toggleStatus').and.resolveTo(true)
    };

    mockRoleUseCase = {
      status: signal('idle'),
      error: signal(null),
      availableRoles: signal([]),
      userRoles: signal([]),
      loadAvailableRoles: jasmine.createSpy('loadAvailableRoles').and.resolveTo(),
      loadRolesForUser: jasmine.createSpy('loadRolesForUser').and.resolveTo(),
      assignRoles: jasmine.createSpy('assignRoles').and.resolveTo(),
      resetState: jasmine.createSpy('resetState')
    };

    mockPermissionService = {
      isCurrentOwner: jasmine.createSpy('isCurrentOwner').and.returnValue(false),
      hasPermission: jasmine.createSpy('hasPermission').and.returnValue(true)
    };

    mockAuthFacade = {
      currentUser: signal({
        id: 'usr-1',
        email: 'admin@empresa.com',
        owner: true
      })
    };

    mockEmployeesService = {
      list: jasmine.createSpy('list').and.returnValue(of({
        success: true,
        data: {
          content: [
            { id: 'emp-1', employeeNumber: 'EMP-001', firstName: 'Admin', lastName: 'Principal' },
            { id: 'emp-2', employeeNumber: 'EMP-002', firstName: 'Juan', lastName: 'Pérez' }
          ]
        }
      }))
    };

    mockToastService = {
      success: jasmine.createSpy('success'),
      error: jasmine.createSpy('error'),
      warning: jasmine.createSpy('warning'),
      info: jasmine.createSpy('info')
    };

    await TestBed.configureTestingModule({
      imports: [UsersPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: UserListUseCase, useValue: mockListUseCase },
        { provide: UserCreateUseCase, useValue: mockCreateUseCase },
        { provide: UserUpdateUseCase, useValue: mockUpdateUseCase },
        { provide: UserStatusUseCase, useValue: mockStatusUseCase },
        { provide: UserRoleUseCase, useValue: mockRoleUseCase },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: AuthFacade, useValue: mockAuthFacade },
        { provide: EmployeesService, useValue: mockEmployeesService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(UsersPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create the component and initialize users and employees', () => {
    expect(component).toBeTruthy();
    expect(mockListUseCase.loadUsers).toHaveBeenCalledWith(0, 20);
    expect(mockEmployeesService.list).toHaveBeenCalled();
  });

  describe('Criterio 3: Listado y cálculo de estadísticas de usuarios', () => {
    it('should correctly calculate user statistics', () => {
      const stats = component.stats();
      expect(stats.total).toBe(4);
      expect(stats.active).toBe(2);
      expect(stats.pending).toBe(1);
      expect(stats.inactive).toBe(1);
      expect(stats.filtered).toBe(4);
    });

    it('should return correct user status and label', () => {
      expect(component.userStatus(mockUsers[0])).toBe('ACTIVE');
      expect(component.userStatusLabel(mockUsers[0])).toBe('Activo');

      expect(component.userStatus(mockUsers[2])).toBe('INACTIVE');
      expect(component.userStatusLabel(mockUsers[2])).toBe('Inactivo');

      expect(component.userStatus(mockUsers[3])).toBe('PENDING');
      expect(component.userStatusLabel(mockUsers[3])).toBe('Pendiente de activación');
    });
  });

  describe('Criterio 3: Filtrado de usuarios por texto y estado', () => {
    it('should filter users by search text query (name or email or employeeNumber)', () => {
      component.searchQuery.set('EMP-002');
      const filtered = component.filteredUsers();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('usr-2');
    });

    it('should filter users by email', () => {
      component.searchQuery.set('pendiente@');
      const filtered = component.filteredUsers();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('usr-4');
    });

    it('should filter users by status selector', () => {
      component.statusFilter.set('INACTIVE');
      const filtered = component.filteredUsers();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('usr-3');
    });

    it('should filter users by combined search and status', () => {
      component.searchQuery.set('Juan');
      component.statusFilter.set('ACTIVE');
      const filtered = component.filteredUsers();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('usr-2');
    });
  });

  describe('Criterio 1: Creación de usuario y vinculación opcional', () => {
    it('should open create modal and reset usecase state', () => {
      component.openCreateModal();
      expect(component.isCreateModalOpen()).toBeTrue();
      expect(mockCreateUseCase.resetState).toHaveBeenCalled();
    });

    it('should invoke userCreateUseCase.createUser when submitting new user', () => {
      const request = {
        email: 'nuevo@empresa.com',
        displayName: 'Nuevo Empleado',
        status: 'ACTIVE' as const,
        employeeId: 'emp-2',
        password: 'Password123!',
        roleIds: ['ROLE-1']
      };

      component.handleCreateUser(request);
      expect(mockCreateUseCase.createUser).toHaveBeenCalledWith(jasmine.objectContaining({
        email: 'nuevo@empresa.com',
        displayName: 'Nuevo Empleado',
        status: 'ACTIVE',
        employeeId: 'emp-2'
      }));
    });
  });

  describe('Criterio 4: Modificación de usuario', () => {
    it('should open edit modal with selected user', () => {
      const user = mockUsers[1];
      component.openEditModal(user);
      expect(component.isEditModalOpen()).toBeTrue();
      expect(component.selectedUserToEdit()).toBe(user);
    });

    it('should invoke userUpdateUseCase.updateUser when submitting updates', () => {
      const user = mockUsers[1];
      component.selectedUserToEdit.set(user);

      const request = {
        email: 'empleado@empresa.com',
        firstName: 'Juan Modificado',
        lastName: 'Pérez',
        displayName: 'Juan P. Modificado',
        employeeId: 'emp-1'
      };

      component.handleUpdateUser(request);
      expect(mockUpdateUseCase.updateUser).toHaveBeenCalledWith('usr-2', request);
    });
  });

  describe('Criterios 5 y 7: Activación, desactivación y protección del propietario', () => {
    it('should prevent deactivating current logged-in user', async () => {
      const currentUser = mockUsers[0]; // usr-1 is logged in
      await component.handleToggleStatus(currentUser);

      expect(mockToastService.warning).toHaveBeenCalledWith('No es posible desactivar al propietario de la organización');
      expect(mockStatusUseCase.toggleStatus).not.toHaveBeenCalled();
    });

    it('should prevent deactivating current user when not owner but matching ID', async () => {
      mockAuthFacade.currentUser.set({ id: 'usr-2', email: 'empleado@empresa.com', owner: false });
      const targetUser = mockUsers[1]; // usr-2 matches currentUser

      await component.handleToggleStatus(targetUser);

      expect(mockToastService.warning).toHaveBeenCalledWith('No puedes desactivar tu propia cuenta activa de administrador');
      expect(mockStatusUseCase.toggleStatus).not.toHaveBeenCalled();
    });

    it('should successfully toggle status for an authorized target user when confirmed', async () => {
      spyOn(window, 'confirm').and.returnValue(true);
      const targetUser = mockUsers[1]; // usr-2 (active = true)

      await component.handleToggleStatus(targetUser);

      expect(mockStatusUseCase.toggleStatus).toHaveBeenCalledWith('usr-2', true);
      expect(mockToastService.success).toHaveBeenCalledWith('Usuario desactivado correctamente');
      expect(mockListUseCase.loadUsers).toHaveBeenCalled();
    });

    it('should not toggle status when user cancels confirmation prompt', async () => {
      spyOn(window, 'confirm').and.returnValue(false);
      const targetUser = mockUsers[1];

      await component.handleToggleStatus(targetUser);

      expect(mockStatusUseCase.toggleStatus).not.toHaveBeenCalled();
    });
  });

  describe('Asignación de roles a usuarios', () => {
    it('debe abrir modal de roles y cargar roles asignados', async () => {
      mockPermissionService.isCurrentOwner.and.returnValue(true);
      mockPermissionService.hasPermission.and.callFake((perm: string) => perm === 'access.users.roles.read');
      const targetUser = mockUsers[1]; // usr-2

      await component.openRoleModal(targetUser);

      expect(component.isRoleModalOpen()).toBeTrue();
      expect(component.selectedUser()).toEqual(targetUser);
      expect(mockRoleUseCase.loadRolesForUser).toHaveBeenCalledWith('usr-2');
    });

    it('no debe abrir modal de roles para el propietario de la organización', async () => {
      mockPermissionService.isCurrentOwner.and.returnValue(true);
      mockAuthFacade.currentUser.set({ id: 'usr-1', email: 'admin@empresa.com', owner: true });
      const ownerUser = mockUsers[0]; // usr-1

      await component.openRoleModal(ownerUser);

      expect(component.isRoleModalOpen()).toBeFalse();
      expect(mockRoleUseCase.loadRolesForUser).not.toHaveBeenCalled();
    });

    it('debe asignar roles, mostrar toast de éxito y cerrar el modal', async () => {
      mockPermissionService.isCurrentOwner.and.returnValue(true);
      mockPermissionService.hasPermission.and.callFake((perm: string) => perm === 'access.users.roles.assign');
      component.selectedUser.set(mockUsers[1]);
      component.isRoleModalOpen.set(true);

      mockRoleUseCase.assignRoles.and.callFake(async () => {
        mockRoleUseCase.status.set('success');
      });

      await component.handleAssignRoles(['r-1', 'r-2']);

      expect(mockRoleUseCase.assignRoles).toHaveBeenCalledWith('usr-2', ['r-1', 'r-2']);
      expect(mockToastService.success).toHaveBeenCalledWith('Roles asignados exitosamente');
      expect(component.isRoleModalOpen()).toBeFalse();
    });

    it('debe mostrar toast de error cuando la asignación falla', async () => {
      mockPermissionService.isCurrentOwner.and.returnValue(true);
      mockPermissionService.hasPermission.and.callFake((perm: string) => perm === 'access.users.roles.assign');
      component.selectedUser.set(mockUsers[1]);
      component.isRoleModalOpen.set(true);

      mockRoleUseCase.assignRoles.and.callFake(async () => {
        mockRoleUseCase.status.set('error');
        mockRoleUseCase.error.set('No se puede asignar un rol inactivo');
      });

      await component.handleAssignRoles(['r-1']);

      expect(mockToastService.error).toHaveBeenCalledWith('No se puede asignar un rol inactivo');
      expect(component.isRoleModalOpen()).toBeTrue();
    });
  });

  describe('Paginación y recarga', () => {
    it('should delegate page changes to userListUseCase', () => {
      component.onPageChange(2);
      expect(mockListUseCase.loadUsers).toHaveBeenCalledWith(2, 20);
    });
  });
});

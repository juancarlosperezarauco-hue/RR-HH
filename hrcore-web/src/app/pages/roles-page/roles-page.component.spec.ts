import { ComponentFixture, TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { RolesPageComponent } from './roles-page.component';
import {
  RoleListUseCase,
  PermissionListUseCase,
  RoleFormUseCase
} from '../../features/role-management';
import { PermissionService } from '../../shared/lib/auth/permission.service';
import { ToastService } from '../../shared/ui/toast/toast.service';
import { SystemPermissionResponse, TenantRoleResponse } from '../../entities/access';

describe('RolesPageComponent - Administración de Roles y Permisos', () => {
  let component: RolesPageComponent;
  let fixture: ComponentFixture<RolesPageComponent>;

  const mockPermissions: SystemPermissionResponse[] = [
    {
      code: 'employees.create',
      module: 'employees',
      description: 'Crear empleado',
      scope: 'TENANT',
      customRoleAssignable: true,
      includedInPlan: true
    },
    {
      code: 'employees.list',
      module: 'employees',
      description: 'Listar empleados',
      scope: 'TENANT',
      customRoleAssignable: true,
      includedInPlan: true
    },
    {
      code: 'shifts.create',
      module: 'shifts',
      description: 'Crear turnos',
      scope: 'TENANT',
      customRoleAssignable: true,
      includedInPlan: false // Excluido del plan básico
    }
  ];

  const mockRoles: TenantRoleResponse[] = [
    {
      id: 'role-owner',
      code: 'OWNER_ADMIN',
      name: 'Propietario de la Organización',
      description: 'Rol de sistema reservado',
      roleType: 'ROOT',
      systemRole: true,
      active: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
      permissionCodes: ['employees.create', 'employees.list', 'shifts.create']
    },
    {
      id: 'role-custom-1',
      code: 'ROL-00001',
      name: 'Analista de Personal',
      description: 'Gestiona empleados y contratos',
      roleType: 'INTERNAL',
      systemRole: false,
      active: true,
      createdAt: '2026-01-05T00:00:00Z',
      updatedAt: '2026-01-05T00:00:00Z',
      permissionCodes: ['employees.create', 'employees.list']
    },
    {
      id: 'role-custom-2',
      code: 'ROL-00002',
      name: 'Supervisor Inactivo',
      description: 'Rol en desuso',
      roleType: 'INTERNAL',
      systemRole: false,
      active: false,
      createdAt: '2026-01-10T00:00:00Z',
      updatedAt: '2026-01-10T00:00:00Z',
      permissionCodes: ['employees.list']
    }
  ];

  let mockRoleListUseCase: {
    data: any;
    status: any;
    error: any;
    loadRoles: jasmine.Spy;
    reloadRoles: jasmine.Spy;
    toggleRoleStatus: jasmine.Spy;
  };

  let mockPermissionListUseCase: {
    data: any;
    status: any;
    error: any;
    loadPermissions: jasmine.Spy;
  };

  let mockRoleFormUseCase: {
    status: any;
    error: any;
    createRole: jasmine.Spy;
    updateRole: jasmine.Spy;
    resetState: jasmine.Spy;
  };

  let mockPermissionService: {
    isCurrentOwner: jasmine.Spy;
    hasPermission: jasmine.Spy;
  };

  let mockToastService: {
    success: jasmine.Spy;
    error: jasmine.Spy;
    warning: jasmine.Spy;
    info: jasmine.Spy;
  };

  beforeEach(async () => {
    mockRoleListUseCase = {
      data: signal(mockRoles),
      status: signal('success'),
      error: signal(null),
      loadRoles: jasmine.createSpy('loadRoles'),
      reloadRoles: jasmine.createSpy('reloadRoles'),
      toggleRoleStatus: jasmine.createSpy('toggleRoleStatus').and.resolveTo(true)
    };

    mockPermissionListUseCase = {
      data: signal(mockPermissions),
      status: signal('success'),
      error: signal(null),
      loadPermissions: jasmine.createSpy('loadPermissions')
    };

    mockRoleFormUseCase = {
      status: signal('idle'),
      error: signal(null),
      createRole: jasmine.createSpy('createRole'),
      updateRole: jasmine.createSpy('updateRole'),
      resetState: jasmine.createSpy('resetState')
    };

    mockPermissionService = {
      isCurrentOwner: jasmine.createSpy('isCurrentOwner').and.returnValue(true),
      hasPermission: jasmine.createSpy('hasPermission').and.returnValue(true)
    };

    mockToastService = {
      success: jasmine.createSpy('success'),
      error: jasmine.createSpy('error'),
      warning: jasmine.createSpy('warning'),
      info: jasmine.createSpy('info')
    };

    await TestBed.configureTestingModule({
      imports: [RolesPageComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: RoleListUseCase, useValue: mockRoleListUseCase },
        { provide: PermissionListUseCase, useValue: mockPermissionListUseCase },
        { provide: RoleFormUseCase, useValue: mockRoleFormUseCase },
        { provide: PermissionService, useValue: mockPermissionService },
        { provide: ToastService, useValue: mockToastService }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(RolesPageComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should initialize component and load roles and permissions', () => {
    expect(component).toBeTruthy();
    expect(mockRoleListUseCase.loadRoles).toHaveBeenCalled();
    expect(mockPermissionListUseCase.loadPermissions).toHaveBeenCalled();
  });

  describe('Criterios 1 y 4: Cálculo de estadísticas y consulta de roles', () => {
    it('should correctly calculate statistics for tenant roles', () => {
      const stats = component.stats();
      expect(stats.total).toBe(3);
      expect(stats.active).toBe(2);
      expect(stats.inactive).toBe(1);
      expect(stats.permissionLinks).toBe(6); // 3 + 2 + 1
      expect(stats.filtered).toBe(3);
    });

    it('should filter roles by text query matching name', () => {
      component.searchQuery.set('Analista');
      const filtered = component.filteredRoles();
      expect(filtered.length).toBe(1);
      expect(filtered[0].name).toBe('Analista de Personal');
    });

    it('should filter roles by permission code', () => {
      component.searchQuery.set('shifts.create');
      const filtered = component.filteredRoles();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('role-owner');
    });

    it('should filter roles by status (active / inactive)', () => {
      component.statusFilter.set('inactive');
      const filtered = component.filteredRoles();
      expect(filtered.length).toBe(1);
      expect(filtered[0].id).toBe('role-custom-2');

      component.statusFilter.set('active');
      expect(component.filteredRoles().length).toBe(2);
    });
  });

  describe('Criterio 2: Creación de rol personalizado', () => {
    it('should open modal for new role and reset state', () => {
      component.openRoleModal();
      expect(component.isModalOpen()).toBeTrue();
      expect(component.roleToEdit()).toBeNull();
      expect(mockRoleFormUseCase.resetState).toHaveBeenCalled();
    });

    it('should delegate role creation to roleFormUseCase.createRole', () => {
      const request = {
        code: 'ROL-00010',
        name: 'Coordinador de Selección',
        description: 'Gestor de reclutamiento',
        permissionCodes: ['employees.list']
      };

      component.handleCreateRole(request);
      expect(mockRoleFormUseCase.createRole).toHaveBeenCalledWith(request);
    });
  });

  describe('Criterio 4: Modificación de roles existentes', () => {
    it('should open edit modal with target custom role', () => {
      const targetRole = mockRoles[1];
      component.openRoleModal(targetRole);

      expect(component.isModalOpen()).toBeTrue();
      expect(component.roleToEdit()).toBe(targetRole);
    });

    it('should prevent opening edit modal for ROOT system role', () => {
      const rootRole = mockRoles[0]; // roleType = 'ROOT'
      component.openRoleModal(rootRole);

      expect(component.isModalOpen()).toBeFalse();
      expect(component.roleToEdit()).toBeNull();
    });

    it('should delegate update to roleFormUseCase.updateRole', () => {
      const request = {
        name: 'Analista de Personal Senior',
        description: 'Descripción actualizada',
        permissionCodes: ['employees.create', 'employees.list']
      };

      component.handleUpdateRole('role-custom-1', request);
      expect(mockRoleFormUseCase.updateRole).toHaveBeenCalledWith('role-custom-1', request);
    });
  });

  describe('Criterio 5: Activación y desactivación de roles', () => {
    it('should toggle role status and reload list with toast notification', async () => {
      await component.handleToggleStatus('role-custom-1', true);

      expect(mockRoleListUseCase.toggleRoleStatus).toHaveBeenCalledWith('role-custom-1', true);
      expect(mockRoleListUseCase.reloadRoles).toHaveBeenCalled();
      expect(mockToastService.success).toHaveBeenCalledWith('Rol desactivado exitosamente');
    });

    it('should display error toast when toggleRoleStatus fails', async () => {
      mockRoleListUseCase.toggleRoleStatus.and.resolveTo(false);
      mockRoleListUseCase.error.set('No se pudo desactivar el rol');

      await component.handleToggleStatus('role-custom-1', true);

      expect(mockToastService.error).toHaveBeenCalledWith('No se pudo desactivar el rol');
    });
  });
});

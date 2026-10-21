import { ComponentFixture, TestBed } from '@angular/core/testing';
import { SimpleChange } from '@angular/core';
import { UserRoleAssignmentComponent } from './user-role-assignment.component';
import { TenantRoleResponse } from '../../../../entities/access';
import { TenantUserResponse } from '../../../../entities/user';

describe('UserRoleAssignmentComponent', () => {
  let component: UserRoleAssignmentComponent;
  let fixture: ComponentFixture<UserRoleAssignmentComponent>;

  const mockUser: TenantUserResponse = {
    id: 'user-123',
    email: 'carlos@example.com',
    firstName: 'Carlos',
    lastName: 'Perez',
    active: true,
    status: 'ACTIVE',
    roles: [{ id: 'role-1', code: 'ROL-00001', name: 'Supervisor', active: true }],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  };

  const mockRole1: TenantRoleResponse = {
    id: 'role-1',
    code: 'ROL-00001',
    name: 'Supervisor',
    description: 'Rol de supervisión',
    roleType: 'INTERNAL',
    systemRole: false,
    active: true,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    permissionCodes: ['employees.list']
  };

  const mockRole2: TenantRoleResponse = {
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

  const mockInactiveRole: TenantRoleResponse = {
    id: 'role-3',
    code: 'ROL-00003',
    name: 'Auditor Antiguo',
    description: 'Rol en desuso',
    roleType: 'INTERNAL',
    systemRole: false,
    active: false,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    permissionCodes: []
  };

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [UserRoleAssignmentComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(UserRoleAssignmentComponent);
    component = fixture.componentInstance;
    component.user = mockUser;
    component.availableRoles = [mockRole1, mockRole2, mockInactiveRole];
    component.assignedRoles = [mockRole1];
    component.ngOnChanges({
      assignedRoles: new SimpleChange([], [mockRole1], true)
    });
    fixture.detectChanges();
  });

  it('debe crearse e inicializar roles seleccionados con assignedRoles', () => {
    expect(component).toBeTruthy();
    expect(component.hasRole('role-1')).toBeTrue();
    expect(component.hasRole('role-2')).toBeFalse();
  });

  it('debe alternar la selección de un rol al activar/desactivar checkbox', () => {
    const eventChecked = { target: { checked: true } } as unknown as Event;
    component.toggleRole('role-2', eventChecked);
    expect(component.hasRole('role-2')).toBeTrue();

    const eventUnchecked = { target: { checked: false } } as unknown as Event;
    component.toggleRole('role-2', eventUnchecked);
    expect(component.hasRole('role-2')).toBeFalse();
  });

  it('debe permitir deseleccionar un rol previamente asignado para revocarlo', () => {
    expect(component.hasRole('role-1')).toBeTrue();
    const eventUnchecked = { target: { checked: false } } as unknown as Event;
    component.toggleRole('role-1', eventUnchecked);
    expect(component.hasRole('role-1')).toBeFalse();
  });

  it('debe emitir submitRoles con la lista de IDs seleccionados', () => {
    spyOn(component.submitRoles, 'emit');
    component.onSubmit();
    expect(component.submitRoles.emit).toHaveBeenCalledWith(['role-1']);

    const eventChecked = { target: { checked: true } } as unknown as Event;
    component.toggleRole('role-2', eventChecked);
    component.onSubmit();
    expect(component.submitRoles.emit).toHaveBeenCalledWith(['role-1', 'role-2']);
  });

  it('no debe emitir submitRoles cuando el estado es loading', () => {
    spyOn(component.submitRoles, 'emit');
    component.status = 'loading';
    component.onSubmit();
    expect(component.submitRoles.emit).not.toHaveBeenCalled();
  });

  it('debe emitir cancel al hacer clic en cancelar', () => {
    spyOn(component.cancel, 'emit');
    component.onCancel();
    expect(component.cancel.emit).toHaveBeenCalled();
  });

  it('no debe emitir cancel cuando el estado es loading', () => {
    spyOn(component.cancel, 'emit');
    component.status = 'loading';
    component.onCancel();
    expect(component.cancel.emit).not.toHaveBeenCalled();
  });
});

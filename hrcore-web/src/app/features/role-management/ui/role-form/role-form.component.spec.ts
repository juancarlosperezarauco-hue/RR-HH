import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ReactiveFormsModule } from '@angular/forms';
import { RoleFormComponent } from './role-form.component';
import { SystemPermissionResponse, TenantRoleResponse } from '../../../../entities/access';

describe('RoleFormComponent', () => {
  let component: RoleFormComponent;
  let fixture: ComponentFixture<RoleFormComponent>;

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
      includedInPlan: false // Excluido del plan
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RoleFormComponent, ReactiveFormsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(RoleFormComponent);
    component = fixture.componentInstance;
    component.permissions = mockPermissions;
    fixture.detectChanges();
  });

  it('should initialize empty form with code control enabled for new role', () => {
    expect(component.roleForm).toBeDefined();
    expect(component.roleForm.get('code')?.enabled).toBeTrue();
    expect(component.roleForm.get('name')?.value).toBe('');
    expect(component.groupedPermissions.length).toBe(2); // employees, shifts
  });

  it('should identify modules and permissions excluded from current subscription plan', () => {
    const shiftsGroup = component.groupedPermissions.find(g => g.module === 'shifts');
    expect(shiftsGroup).toBeDefined();
    expect(shiftsGroup?.includedInPlan).toBeFalse();
    expect(component.isModuleDisabled(shiftsGroup!)).toBeTrue();

    const shiftPerm = shiftsGroup?.permissions.find(p => p.code === 'shifts.create');
    expect(shiftPerm).toBeDefined();
    expect(component.isPermissionDisabled(shiftPerm!)).toBeTrue();
  });

  it('should not allow selecting permissions that are excluded by plan', () => {
    const shiftPerm = mockPermissions.find(p => p.code === 'shifts.create')!;
    const dummyEvent = { target: { checked: true } } as unknown as Event;

    component.togglePermission(shiftPerm, dummyEvent);
    expect(component.isPermissionSelected('shifts.create')).toBeFalse();
  });

  it('should emit submitCreate with code, name, description and authorized permissions', () => {
    spyOn(component.submitCreate, 'emit');

    component.roleForm.patchValue({
      code: 'ROL-00003',
      name: 'Gestor Básico',
      description: 'Rol para empleados'
    });

    // Seleccionar permiso válido
    const empPerm = mockPermissions.find(p => p.code === 'employees.create')!;
    const dummyEvent = { target: { checked: true } } as unknown as Event;
    component.togglePermission(empPerm, dummyEvent);

    component.onSubmit();

    expect(component.submitCreate.emit).toHaveBeenCalledWith({
      code: 'ROL-00003',
      name: 'Gestor Básico',
      description: 'Rol para empleados',
      permissionCodes: ['employees.create']
    });
  });

  it('should patch existing role values and disable code field when roleToEdit is provided', () => {
    const existingRole: TenantRoleResponse = {
      id: 'role-123',
      code: 'ROL-00001',
      name: 'Rol Existente',
      description: 'Descripción previa',
      roleType: 'INTERNAL',
      systemRole: false,
      active: true,
      createdAt: '2026-01-01T00:00:00Z',
      updatedAt: '2026-01-01T00:00:00Z',
      permissionCodes: ['employees.list']
    };

    component.roleToEdit = existingRole;
    component.ngOnChanges({
      roleToEdit: {
        currentValue: existingRole,
        previousValue: null,
        firstChange: false,
        isFirstChange: () => false
      }
    });

    expect(component.roleForm.get('code')?.value).toBe('ROL-00001');
    expect(component.roleForm.get('code')?.disabled).toBeTrue();
    expect(component.roleForm.get('name')?.value).toBe('Rol Existente');
    expect(component.isPermissionSelected('employees.list')).toBeTrue();
  });
});

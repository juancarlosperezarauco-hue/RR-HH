import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CreateTenantRoleRequest, SystemPermissionResponse, TenantRoleResponse, UpdateTenantRoleRequest } from '../../../../entities/access';

interface PermissionGroup {
  module: string;
  permissions: SystemPermissionResponse[];
}

@Component({
  selector: 'app-role-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './role-form.component.html'
})
export class RoleFormComponent implements OnInit, OnChanges {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() roleToEdit: TenantRoleResponse | null = null;
  @Input() permissions: SystemPermissionResponse[] = [];
  
  @Output() submitCreate = new EventEmitter<CreateTenantRoleRequest>();
  @Output() submitUpdate = new EventEmitter<{ id: string, request: UpdateTenantRoleRequest }>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  roleForm!: FormGroup;
  groupedPermissions: PermissionGroup[] = [];

  ngOnInit(): void {
    this.initForm();
    this.groupPermissions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['permissions'] && !changes['permissions'].firstChange) {
      this.groupPermissions();
    }
    
    // Si roleToEdit cambia después de inicializar el form
    if (changes['roleToEdit'] && this.roleForm) {
      this.patchFormValues();
    }
  }

  private initForm(): void {
    this.roleForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      description: ['', [Validators.maxLength(255)]],
      permissionCodes: this.fb.array([])
    });

    if (this.roleToEdit) {
      this.patchFormValues();
    }
  }

  private patchFormValues(): void {
    if (!this.roleForm) return;
    
    if (this.roleToEdit) {
      this.roleForm.patchValue({
        name: this.roleToEdit.name,
        description: this.roleToEdit.description || ''
      });
      // Limpiamos y agregamos los permisos
      const permissionsArray = this.roleForm.get('permissionCodes') as FormArray;
      permissionsArray.clear();
      
      this.roleToEdit.permissionCodes.forEach(code => {
        permissionsArray.push(new FormControl(code));
      });
    } else {
      this.roleForm.reset();
      (this.roleForm.get('permissionCodes') as FormArray).clear();
    }
  }

  private groupPermissions(): void {
    if (!this.permissions || this.permissions.length === 0) return;

    const grouped = this.permissions.reduce((acc, perm) => {
      const moduleName = perm.module;
      if (!acc[moduleName]) {
        acc[moduleName] = [];
      }
      acc[moduleName].push(perm);
      return acc;
    }, {} as Record<string, SystemPermissionResponse[]>);

    this.groupedPermissions = Object.keys(grouped).map(key => ({
      module: key,
      permissions: grouped[key]
    })).sort((a, b) => a.module.localeCompare(b.module));
  }

  private get permissionsArray(): FormArray {
    return this.roleForm.get('permissionCodes') as FormArray;
  }

  private syncPermissionCodes(codes: string[]): void {
    const uniqueCodes = Array.from(new Set(codes.filter(code => !!code && code.trim().length > 0)));
    this.permissionsArray.clear();
    uniqueCodes.forEach(code => {
      this.permissionsArray.push(new FormControl(code));
    });
  }

  private getAllPermissionCodes(): string[] {
    return this.groupedPermissions.reduce((codes: string[], group) => {
      group.permissions.forEach(permission => codes.push(permission.code));
      return codes;
    }, []);
  }

  // Helpers para Checkboxes/Switches
  isPermissionSelected(code: string): boolean {
    return this.permissionsArray.value.includes(code);
  }

  togglePermission(code: string, event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;

    if (isChecked) {
      if (!this.isPermissionSelected(code)) {
        this.permissionsArray.push(new FormControl(code));
      }
    } else {
      const index = this.permissionsArray.controls.findIndex(x => x.value === code);
      if (index >= 0) {
        this.permissionsArray.removeAt(index);
      }
    }
  }

  areAllPermissionsSelected(): boolean {
    const totalPermissions = this.getAllPermissionCodes().length;
    if (totalPermissions === 0) return false;
    return this.permissionsArray.length === totalPermissions;
  }

  areSomePermissionsSelected(): boolean {
    return this.permissionsArray.length > 0 && !this.areAllPermissionsSelected();
  }

  toggleAllPermissions(event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.syncPermissionCodes(this.getAllPermissionCodes());
    } else {
      this.permissionsArray.clear();
    }
  }

  // Select all inside a module
  isModuleFullySelected(modulePermissions: SystemPermissionResponse[]): boolean {
    if (modulePermissions.length === 0) return false;
    return modulePermissions.every(p => this.isPermissionSelected(p.code));
  }

  isModuleIndeterminate(modulePermissions: SystemPermissionResponse[]): boolean {
    if (modulePermissions.length === 0) return false;
    const selectedCount = modulePermissions.filter(permission => this.isPermissionSelected(permission.code)).length;
    return selectedCount > 0 && selectedCount < modulePermissions.length;
  }

  toggleModule(modulePermissions: SystemPermissionResponse[], event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;

    if (isChecked) {
      const merged = [...this.permissionsArray.value, ...modulePermissions.map(p => p.code)];
      this.syncPermissionCodes(merged);
      return;
    }

    const codesToRemove = new Set(modulePermissions.map(p => p.code));
    const remaining = this.permissionsArray.value.filter((code: string) => !codesToRemove.has(code));
    this.syncPermissionCodes(remaining);
  }

  onSubmit(): void {
    if (this.roleForm.valid && this.status !== 'loading') {
      const formValue = this.roleForm.value;
      
      if (this.roleToEdit) {
        const request: UpdateTenantRoleRequest = {
          name: formValue.name,
          description: formValue.description,
          permissionCodes: formValue.permissionCodes
        };
        this.submitUpdate.emit({ id: this.roleToEdit.id, request });
      } else {
        const request: CreateTenantRoleRequest = {
          name: formValue.name,
          description: formValue.description,
          permissionCodes: formValue.permissionCodes
        };
        this.submitCreate.emit(request);
      }
    } else {
      this.roleForm.markAllAsTouched();
    }
  }

  onCancel(): void {
    if (this.status !== 'loading') {
      this.cancel.emit();
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.roleForm?.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
}

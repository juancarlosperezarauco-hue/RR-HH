import { Component, EventEmitter, inject, Input, OnChanges, OnInit, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormArray, FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { CreateTenantRoleRequest, SystemPermissionResponse, TenantRoleResponse, UpdateTenantRoleRequest } from '../../../../entities/access';

export interface PermissionGroup {
  module: string;
  permissions: SystemPermissionResponse[];
  includedInPlan: boolean;
}

@Component({
  selector: 'app-role-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './role-form.component.html'
})
export class RoleFormComponent implements OnInit, OnChanges {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() error: string | null = null;
  @Input() roleToEdit: TenantRoleResponse | null = null;
  @Input() permissions: SystemPermissionResponse[] = [];
  
  @Output() submitCreate = new EventEmitter<CreateTenantRoleRequest>();
  @Output() submitUpdate = new EventEmitter<{ id: string, request: UpdateTenantRoleRequest }>();
  @Output() cancel = new EventEmitter<void>();

  private readonly fb = inject(FormBuilder);

  roleForm!: FormGroup;
  groupedPermissions: PermissionGroup[] = [];
  permissionSearch = '';
  permissionFilter: 'all' | 'selected' | 'available' = 'all';
  private readonly expandedModules = new Set<string>();

  ngOnInit(): void {
    this.initForm();
    this.groupPermissions();
  }

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['permissions'] && !changes['permissions'].firstChange) {
      this.groupPermissions();
    }
    
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
      const permissionsArray = this.roleForm.get('permissionCodes') as FormArray;
      permissionsArray.clear();
      
      this.roleToEdit.permissionCodes.forEach(code => {
        permissionsArray.push(new FormControl(code));
      });
      this.syncExpandedModulesToSelection();
    } else {
      this.roleForm.reset();
      (this.roleForm.get('permissionCodes') as FormArray).clear();
    }
  }

  private groupPermissions(): void {
    if (!this.permissions || this.permissions.length === 0) {
      this.groupedPermissions = [];
      return;
    }

    const grouped = this.permissions.reduce((acc, perm) => {
      const moduleName = perm.module;
      if (!acc[moduleName]) {
        acc[moduleName] = [];
      }
      if (perm.scope === 'TENANT' && perm.customRoleAssignable) {
        acc[moduleName].push(perm);
      }
      return acc;
    }, {} as Record<string, SystemPermissionResponse[]>);

    this.groupedPermissions = Object.keys(grouped).map(key => {
      const perms = grouped[key];
      const included = perms.some(p => p.includedInPlan !== false);
      return {
        module: key,
        permissions: perms,
        includedInPlan: included
      };
    }).sort((a, b) => a.module.localeCompare(b.module));

    this.syncExpandedModulesToSelection();
  }

  private syncExpandedModulesToSelection(): void {
    this.expandedModules.clear();
    this.groupedPermissions.forEach(group => {
      if (group.permissions.some(permission => this.isPermissionSelected(permission.code))) {
        this.expandedModules.add(group.module);
      }
    });
  }

  visiblePermissionGroups(): PermissionGroup[] {
    const search = this.permissionSearch.trim().toLowerCase();
    return this.groupedPermissions
      .map(group => {
        const moduleMatches = !search || this.getModuleLabel(group.module).toLowerCase().includes(search);
        const permissions = group.permissions.filter(permission => {
          const matchesSearch = moduleMatches
            || this.getPermissionLabel(permission).toLowerCase().includes(search)
            || this.getPermissionDescription(permission).toLowerCase().includes(search)
            || permission.code.toLowerCase().includes(search);
          const matchesFilter = this.permissionFilter === 'all'
            || (this.permissionFilter === 'selected' && this.isPermissionSelected(permission.code))
            || (this.permissionFilter === 'available' && permission.includedInPlan !== false);
          return matchesSearch && matchesFilter;
        });
        return { ...group, permissions };
      })
      .filter(group => group.permissions.length > 0);
  }

  setPermissionSearch(event: Event): void {
    this.permissionSearch = (event.target as HTMLInputElement).value;
  }

  setPermissionFilter(filter: 'all' | 'selected' | 'available'): void {
    this.permissionFilter = filter;
  }

  isModuleExpanded(module: string): boolean {
    return this.expandedModules.has(module);
  }

  toggleModuleExpanded(module: string): void {
    if (this.expandedModules.has(module)) {
      this.expandedModules.delete(module);
    } else {
      this.expandedModules.add(module);
    }
  }

  selectedPermissionCount(): number {
    return this.permissionsArray.value.length;
  }

  availablePermissionCount(): number {
    return this.getAllPermissionCodes().length;
  }

  selectableCountForGroup(group: PermissionGroup): number {
    return group.permissions.filter(permission => permission.includedInPlan !== false).length;
  }

  selectedCountForGroup(group: PermissionGroup): number {
    return group.permissions.filter(permission => permission.includedInPlan !== false && this.isPermissionSelected(permission.code)).length;
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
      group.permissions.forEach(permission => {
        if (permission.includedInPlan !== false) {
          codes.push(permission.code);
        }
      });
      return codes;
    }, []);
  }

  isPermissionDisabled(perm: SystemPermissionResponse): boolean {
    return perm.includedInPlan === false;
  }

  isModuleDisabled(group: PermissionGroup): boolean {
    return !group.includedInPlan || group.permissions.every(p => p.includedInPlan === false);
  }

  isPermissionSelected(code: string): boolean {
    return this.permissionsArray.value.includes(code);
  }

  togglePermission(perm: SystemPermissionResponse, event: Event): void {
    if (this.isPermissionDisabled(perm)) {
      (event.target as HTMLInputElement).checked = false;
      return;
    }

    const code = perm.code;
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

  isModuleFullySelected(group: PermissionGroup): boolean {
    const valid = group.permissions.filter(p => p.includedInPlan !== false);
    if (valid.length === 0) return false;
    return valid.every(p => this.isPermissionSelected(p.code));
  }

  isModuleIndeterminate(group: PermissionGroup): boolean {
    const valid = group.permissions.filter(p => p.includedInPlan !== false);
    if (valid.length === 0) return false;
    const selectedCount = valid.filter(p => this.isPermissionSelected(p.code)).length;
    return selectedCount > 0 && selectedCount < valid.length;
  }

  toggleModule(group: PermissionGroup, event: Event): void {
    if (this.isModuleDisabled(group)) {
      (event.target as HTMLInputElement).checked = false;
      return;
    }

    const isChecked = (event.target as HTMLInputElement).checked;
    const validCodes = group.permissions
      .filter(p => p.includedInPlan !== false)
      .map(p => p.code);

    if (isChecked) {
      const merged = [...this.permissionsArray.value, ...validCodes];
      this.syncPermissionCodes(merged);
      return;
    }

    const codesToRemove = new Set(validCodes);
    const remaining = this.permissionsArray.value.filter((code: string) => !codesToRemove.has(code));
    this.syncPermissionCodes(remaining);
  }

  onSubmit(): void {
    if (this.roleForm.valid && this.status !== 'loading') {
      const formValue = this.roleForm.getRawValue();
      const selected: string[] = (formValue.permissionCodes ?? []) as string[];
      const allowed = this.getAllPermissionCodes();
      const permissionCodes: string[] = [...new Set(selected.filter(code => allowed.includes(code)))];
      
      const name = String(formValue.name ?? '').trim();
      
      if (this.roleToEdit) {
        const request: UpdateTenantRoleRequest = {
          name,
          description: String(formValue.description ?? '').trim() || null,
          permissionCodes
        };
        this.submitUpdate.emit({ id: this.roleToEdit.id, request });
      } else {
        const request: CreateTenantRoleRequest = {
          name,
          description: String(formValue.description ?? '').trim() || null,
          permissionCodes
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

  getModuleLabel(moduleKey: string): string {
    const key = (moduleKey || '').trim().toLowerCase();
    const map: Record<string, string> = {
      tenantsettings: 'Configuración de la Empresa',
      settings: 'Configuración de la Empresa',
      'tenant-settings': 'Configuración de la Empresa',
      shifts: 'Turnos y Horarios de Trabajo',
      attendance: 'Control de Asistencias y Marcaciones',
      departments: 'Departamentos y Áreas',
      positions: 'Cargos y Puestos de Trabajo',
      employees: 'Gestión de Empleados',
      contracts: 'Contratos Laborales',
      employeefiles: 'Expedientes Laborales',
      employee_files: 'Expedientes Laborales',
      dossiers: 'Expedientes Laborales',
      absences: 'Gestión de Ausencias y Vacaciones',
      leaves: 'Gestión de Ausencias y Vacaciones',
      incidents: 'Incidencias Laborales',
      dashboard: 'Panel de Control y Resumen',
      users: 'Usuarios y Cuentas de Acceso',
      roles: 'Roles y Permisos (RBAC)',
      access: 'Roles y Permisos (RBAC)',
      notifications: 'Notificaciones del Sistema',
      backups: 'Respaldos y Seguridad',
      audit: 'Bitácora de Auditoría',
      reports: 'Reportes y Analíticas de RRHH',
      hr: 'Reportes y Analíticas de RRHH',
      billing: 'Facturación y Suscripción'
    };
    return map[key] || (key.charAt(0).toUpperCase() + key.slice(1));
  }

  getPermissionLabel(perm: SystemPermissionResponse): string {
    const code = (perm?.code || '').trim();
    const map: Record<string, string> = {
      // Access / Seguridad y Roles
      'access.permissions.read': 'Consultar catálogo de permisos del sistema',
      'access.roles.activate': 'Activar roles de la organización',
      'access.roles.create': 'Crear nuevos roles en la organización',
      'access.roles.deactivate': 'Desactivar roles de la organización',
      'access.roles.detail': 'Ver detalle de roles y sus permisos',
      'access.roles.read': 'Consultar listado de roles',
      'access.roles.update': 'Editar roles y permisos asignados',
      'access.users.roles.assign': 'Asignar y desvincular roles a usuarios',
      'access.users.roles.read': 'Consultar roles asignados a usuarios',

      // Attendance / Asistencia y Marcaciones
      'attendance.adjustments.approve': 'Aprobar solicitudes de ajuste de asistencia',
      'attendance.adjustments.cancel': 'Cancelar solicitudes de ajuste de asistencia',
      'attendance.adjustments.create': 'Crear solicitud de ajuste de marcación',
      'attendance.adjustments.detail': 'Ver detalle de solicitud de ajuste de asistencia',
      'attendance.adjustments.list': 'Listar solicitudes de ajuste de asistencia',
      'attendance.adjustments.reject': 'Rechazar solicitud de ajuste de asistencia',
      'attendance.overtime.approve': 'Aprobar solicitudes de horas extras',
      'attendance.overtime.cancel': 'Cancelar solicitud de horas extras',
      'attendance.overtime.create': 'Registrar solicitud de horas extras',
      'attendance.overtime.detail': 'Ver detalle de solicitud de horas extras',
      'attendance.overtime.list': 'Listar solicitudes de horas extras',
      'attendance.overtime.reject': 'Rechazar solicitud de horas extras',
      'attendance.punches.create': 'Registrar marcaciones de entrada y salida',
      'attendance.punches.detail': 'Ver detalle de marcación de asistencia',
      'attendance.punches.list': 'Listar marcaciones de asistencia',
      'attendance.punches.manage': 'Gestionar y corregir marcaciones de asistencia',
      'attendance.records.calculate': 'Ejecutar cálculo diario de asistencias',
      'attendance.records.detail': 'Ver detalle de registro diario de asistencia',
      'attendance.records.list': 'Listar registros diarios de asistencia',
      'attendance.team.read': 'Consultar asistencia del equipo de trabajo',

      // Audit / Auditoría
      'audit.events.read': 'Consultar bitácora de auditoría e historial',

      // Backups / Respaldos
      'backups.create': 'Crear respaldo de seguridad de datos',
      'backups.detail': 'Ver detalle de respaldo de seguridad',
      'backups.download': 'Descargar archivo de respaldo',
      'backups.list': 'Listar historial de respaldos de seguridad',
      'backups.restore': 'Restaurar base de datos desde respaldo',

      // Billing / Suscripciones
      'billing.subscription.manage': 'Gestionar suscripción y facturación del tenant',

      // Contracts / Contratos Laborales
      'contracts.create': 'Crear nuevos contratos laborales',
      'contracts.detail': 'Ver detalle de contrato laboral',
      'contracts.document.download': 'Descargar documento de contrato en PDF',
      'contracts.history.read': 'Consultar historial de contratos del empleado',
      'contracts.list': 'Listar contratos laborales',
      'contracts.renew': 'Renovar contratos laborales',
      'contracts.status.update': 'Actualizar estado de contrato laboral',
      'contracts.update': 'Editar información de contratos laborales',

      // Dashboard
      'dashboard.tenant.summary.read': 'Visualizar métricas del panel principal',

      // Departments / Departamentos
      'departments.activate': 'Activar departamentos',
      'departments.create': 'Crear nuevos departamentos',
      'departments.deactivate': 'Desactivar departamentos',
      'departments.detail': 'Ver detalle del departamento',
      'departments.history.read': 'Consultar historial de departamentos',
      'departments.list': 'Listar departamentos de la empresa',
      'departments.update': 'Editar datos de departamentos',

      // Employee Files / Expedientes
      'employee_files.create': 'Crear expedientes de colaboradores',
      'employee_files.detail': 'Ver detalle de expediente de colaborador',
      'employee_files.documents.confidential.read': 'Consultar documentos confidenciales del expediente',
      'employee_files.documents.create': 'Registrar documento en expediente',
      'employee_files.documents.detail': 'Ver detalle de documento en expediente',
      'employee_files.documents.download': 'Descargar documentos del expediente',
      'employee_files.documents.list': 'Listar documentos del expediente',
      'employee_files.documents.update': 'Editar documento del expediente',
      'employee_files.documents.upload': 'Subir archivos al expediente del colaborador',
      'employee_files.documents.verify': 'Verificar documentos del expediente',
      'employee_files.list': 'Listar expedientes de colaboradores',
      'employee_files.update': 'Actualizar datos del expediente',

      // Employees / Empleados
      'employees.assignments.cancel': 'Cancelar asignación de puesto del empleado',
      'employees.assignments.close': 'Finalizar asignación de puesto del empleado',
      'employees.assignments.create': 'Crear asignación de puesto para empleado',
      'employees.assignments.detail': 'Ver detalle de asignación de puesto',
      'employees.assignments.list': 'Listar asignaciones de puesto de empleados',
      'employees.assignments.update': 'Editar asignación de puesto de empleado',
      'employees.contacts.create': 'Crear contacto de emergencia para empleado',
      'employees.contacts.delete': 'Eliminar contacto de emergencia',
      'employees.contacts.detail': 'Ver detalle de contacto de emergencia',
      'employees.contacts.list': 'Listar contactos de emergencia del empleado',
      'employees.contacts.update': 'Editar contacto de emergencia del empleado',
      'employees.create': 'Registrar nuevos empleados',
      'employees.detail': 'Ver ficha completa de empleado',
      'employees.list': 'Listar empleados de la organización',
      'employees.rehire': 'Recontratar empleado',
      'employees.status.update': 'Actualizar estado laboral de empleado',
      'employees.team.read': 'Consultar colaboradores de su equipo de trabajo',
      'employees.terminate': 'Registrar desvinculación o baja de empleado',
      'employees.termination.read': 'Consultar datos de desvinculación laboral',
      'employees.transfer': 'Transferir empleado de puesto o departamento',
      'employees.update': 'Editar datos de empleados',

      // Reports / Reportes de RRHH
      'hr.reports.attendance.read': 'Consultar reportes analíticos de asistencia',
      'hr.reports.contracts.read': 'Consultar reportes analíticos de contratos',
      'hr.reports.employees.read': 'Consultar reportes analíticos de empleados',
      'hr.reports.export': 'Exportar reportes de recursos humanos a Excel/PDF',
      'hr.reports.incidents.read': 'Consultar reportes analíticos de incidencias',
      'hr.reports.leaves.read': 'Consultar reportes analíticos de ausencias',

      // Incidents / Incidencias
      'incidents.actions.create': 'Registrar acción sobre incidencia',
      'incidents.actions.list': 'Listar acciones registradas en incidencias',
      'incidents.actions.update': 'Editar acción de incidencia',
      'incidents.assign': 'Asignar responsable a incidencia',
      'incidents.attachments.download': 'Descargar archivos adjuntos de incidencias',
      'incidents.attachments.list': 'Listar archivos adjuntos de incidencias',
      'incidents.attachments.upload': 'Subir archivos adjuntos a incidencia',
      'incidents.cancel': 'Cancelar incidencia laboral',
      'incidents.close': 'Cerrar incidencia resuelta',
      'incidents.create': 'Registrar nueva incidencia laboral',
      'incidents.detail': 'Ver detalle de incidencia laboral',
      'incidents.history.read': 'Consultar historial de incidencias laborales',
      'incidents.list': 'Listar incidencias laborales',
      'incidents.resolve': 'Resolver incidencia laboral',
      'incidents.review': 'Revisar incidencias reportadas',
      'incidents.update': 'Editar información de incidencia laboral',

      // Leaves / Ausencias y Vacaciones
      'leaves.attachments.download': 'Descargar comprobantes y justificaciones de ausencia',
      'leaves.attachments.list': 'Listar comprobantes de ausencia',
      'leaves.attachments.upload': 'Subir comprobante o justificación de ausencia',
      'leaves.balances.adjust': 'Ajustar saldo de días de vacaciones',
      'leaves.balances.create': 'Asignar saldo inicial de vacaciones',
      'leaves.balances.detail': 'Ver detalle de saldo de vacaciones del empleado',
      'leaves.balances.list': 'Listar saldos de vacaciones de empleados',
      'leaves.history.read': 'Consultar historial de ausencias y licencias',
      'leaves.requests.approve': 'Aprobar solicitudes de ausencia',
      'leaves.requests.approve.global': 'Aprobación global de solicitudes de ausencia',
      'leaves.requests.cancel': 'Cancelar solicitud de ausencia',
      'leaves.requests.create': 'Crear solicitud de ausencia o vacaciones',
      'leaves.requests.detail': 'Ver detalle de solicitud de ausencia',
      'leaves.requests.list': 'Listar solicitudes de ausencia',
      'leaves.requests.reject': 'Rechazar solicitud de ausencia',
      'leaves.requests.submit': 'Enviar solicitud de ausencia a revisión',
      'leaves.requests.update': 'Editar solicitud de ausencia',
      'leaves.team.read': 'Consultar ausencias de los colaboradores del equipo',
      'leaves.types.activate': 'Activar tipos de ausencia',
      'leaves.types.create': 'Crear tipos de ausencia',
      'leaves.types.deactivate': 'Desactivar tipos de ausencia',
      'leaves.types.detail': 'Ver detalle del tipo de ausencia',
      'leaves.types.list': 'Listar catálogo de tipos de ausencia',
      'leaves.types.update': 'Editar tipos de ausencia',

      // Notifications / Notificaciones
      'notifications.deliveries.read': 'Consultar registro de envíos de notificaciones',
      'notifications.templates.detail': 'Ver detalle de plantilla de notificación',
      'notifications.templates.read': 'Consultar plantillas de notificación',

      // Positions / Cargos
      'positions.activate': 'Activar cargos o puestos de trabajo',
      'positions.create': 'Crear nuevos cargos',
      'positions.deactivate': 'Desactivar cargos',
      'positions.detail': 'Ver detalle del cargo',
      'positions.list': 'Listar cargos y puestos de trabajo',
      'positions.requirements.create': 'Crear requisito para cargo',
      'positions.requirements.delete': 'Eliminar requisito del cargo',
      'positions.requirements.list': 'Listar requisitos del cargo',
      'positions.requirements.update': 'Editar requisitos del cargo',
      'positions.update': 'Editar información del cargo',

      // Shifts / Turnos y Horarios
      'shifts.activate': 'Activar turnos laborales',
      'shifts.assignments.cancel': 'Cancelar asignación de turnos a empleados',
      'shifts.assignments.close': 'Cerrar asignación de turnos',
      'shifts.assignments.create': 'Asignar turnos a empleados',
      'shifts.assignments.detail': 'Ver detalle de asignación de turno',
      'shifts.assignments.list': 'Listar asignaciones de turnos',
      'shifts.assignments.update': 'Editar asignación de turnos',
      'shifts.breaks.create': 'Crear descansos dentro del turno',
      'shifts.breaks.delete': 'Eliminar descansos del turno',
      'shifts.breaks.detail': 'Ver detalle del descanso',
      'shifts.breaks.list': 'Listar descansos configurados en turnos',
      'shifts.breaks.update': 'Editar descansos del turno',
      'shifts.calendar.create': 'Crear excepción en calendario laboral',
      'shifts.calendar.delete': 'Eliminar excepción del calendario',
      'shifts.calendar.detail': 'Ver detalle de excepción del calendario',
      'shifts.calendar.list': 'Listar excepciones del calendario laboral',
      'shifts.calendar.update': 'Editar excepción del calendario',
      'shifts.create': 'Crear turnos laborales',
      'shifts.deactivate': 'Desactivar turnos laborales',
      'shifts.detail': 'Ver detalle del turno laboral',
      'shifts.list': 'Listar turnos laborales',
      'shifts.schedule.read': 'Consultar cronograma general de turnos',
      'shifts.schedule.self.read': 'Consultar mi propio horario de trabajo',
      'shifts.update': 'Editar turnos laborales',

      // Tenant Settings / Configuración de la Empresa
      'tenant-settings.read': 'Consultar configuración general de la empresa',
      'tenant-settings.update': 'Editar configuración general de la empresa',
      'tenantsettings.read': 'Consultar configuración general de la empresa',
      'tenantsettings.update': 'Editar configuración general de la empresa',

      // Users / Usuarios
      'users.activate': 'Activar acceso a cuentas de usuario',
      'users.create': 'Crear nuevas cuentas de usuario',
      'users.deactivate': 'Desactivar cuentas de usuario',
      'users.detail': 'Ver detalle completo del usuario',
      'users.list': 'Listar cuentas de usuario',
      'users.update': 'Editar información de usuarios'
    };

    if (map[code]) return map[code];

    const parts = code.split('.');
    const action = parts[parts.length - 1];
    const resourceParts = parts.slice(0, parts.length - 1);
    
    const actionMap: Record<string, string> = {
      create: 'Crear',
      read: 'Consultar',
      list: 'Listar',
      detail: 'Ver detalle de',
      view: 'Ver',
      update: 'Editar',
      delete: 'Eliminar',
      activate: 'Activar',
      deactivate: 'Desactivar',
      download: 'Descargar',
      upload: 'Subir',
      review: 'Revisar',
      approve: 'Aprobar',
      reject: 'Rechazar',
      cancel: 'Cancelar',
      submit: 'Enviar',
      assign: 'Asignar',
      manage: 'Gestionar',
      renew: 'Renovar',
      restore: 'Restaurar',
      calculate: 'Calcular',
      export: 'Exportar'
    };

    const nounMap: Record<string, string> = {
      types: 'tipos de ausencia',
      type: 'tipo de ausencia',
      requests: 'solicitudes de ausencia',
      request: 'solicitud de ausencia',
      team: 'equipo de trabajo',
      user: 'usuario',
      users: 'usuarios',
      role: 'rol',
      roles: 'roles',
      permissions: 'permisos',
      permission: 'permiso',
      assignments: 'asignaciones de turnos',
      assignment: 'asignación de turnos',
      breaks: 'descansos de turnos',
      break: 'descanso de turno',
      calendar: 'calendario laboral',
      punches: 'marcaciones de asistencia',
      punch: 'marcación',
      records: 'registros de asistencia',
      record: 'registro de asistencia',
      overtime: 'horas extras',
      adjustments: 'ajustes de asistencia',
      adjustment: 'ajuste de asistencia',
      balances: 'saldos de vacaciones',
      balance: 'saldo de vacaciones',
      attachments: 'archivos adjuntos',
      attachment: 'archivo adjunto',
      documents: 'documentos de expediente',
      document: 'documento',
      contacts: 'contactos de emergencia',
      contact: 'contacto de emergencia',
      deliveries: 'entregas de notificación',
      delivery: 'entrega de notificación',
      templates: 'plantillas de notificación',
      template: 'plantilla de notificación',
      requirements: 'requisitos de puesto',
      requirement: 'requisito de puesto',
      reports: 'reportes de recursos humanos',
      report: 'reporte de recursos humanos',
      actions: 'acciones de incidencia',
      action: 'acción de incidencia',
      settings: 'configuración de empresa',
      subscription: 'suscripción',
      schedule: 'cronograma de horarios',
      history: 'historial',
      summary: 'resumen',
      confidential: 'confidencial'
    };

    const translatedAction = actionMap[action] || (action.charAt(0).toUpperCase() + action.slice(1));
    const translatedNouns = resourceParts
      .map(part => nounMap[part] || part)
      .filter((part, idx, arr) => arr.indexOf(part) === idx)
      .join(' ');

    return `${translatedAction} ${translatedNouns}`.trim();
  }

  getPermissionDescription(perm: SystemPermissionResponse): string {
    const label = this.getPermissionLabel(perm);
    return `Permite a los usuarios asignados a este rol ${label.toLowerCase()}.`;
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.roleForm?.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }
}

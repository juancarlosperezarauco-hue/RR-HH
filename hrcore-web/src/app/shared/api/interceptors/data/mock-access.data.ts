export interface MockPermission {
  id: string;
  code: string;
  name: string;
  module: string;
  description: string;
}

export interface MockRole {
  id: string;
  name: string;
  description: string;
  active: boolean;
  systemRole: boolean;
  permissionCount: number;
  permissions: MockPermission[];
  createdAt: string;
  updatedAt: string;
}

export const MOCK_PERMISSIONS: MockPermission[] = [
  { id: 'p-1', code: 'users.list', name: 'Listar usuarios', module: 'USUARIOS', description: 'Permite consultar el listado de usuarios del tenant' },
  { id: 'p-2', code: 'users.create', name: 'Crear usuario', module: 'USUARIOS', description: 'Permite crear nuevas cuentas de usuario' },
  { id: 'p-3', code: 'users.update', name: 'Modificar usuario', module: 'USUARIOS', description: 'Permite actualizar datos de usuarios' },
  { id: 'p-4', code: 'access.roles.read', name: 'Ver roles', module: 'SEGURIDAD', description: 'Permite consultar los roles y perfiles' },
  { id: 'p-5', code: 'access.roles.write', name: 'Gestionar roles', module: 'SEGURIDAD', description: 'Permite crear y modificar roles' },
  { id: 'p-6', code: 'access.permissions.read', name: 'Ver permisos', module: 'SEGURIDAD', description: 'Permite listar los permisos del sistema' },
  { id: 'p-7', code: 'contracts.list', name: 'Listar contratos', module: 'CONTRATOS', description: 'Permite consultar contratos laborales' },
  { id: 'p-8', code: 'contracts.create', name: 'Crear contrato', module: 'CONTRATOS', description: 'Permite emitir contratos laborales' },
  { id: 'p-9', code: 'employee_files.list', name: 'Listar expedientes', module: 'EXPEDIENTES', description: 'Permite consultar expedientes del personal' },
  { id: 'p-10', code: 'employee_files.create', name: 'Crear expediente', module: 'EXPEDIENTES', description: 'Permite aperturar expedientes laborales' },
  { id: 'p-11', code: 'employee_files.detail', name: 'Ver detalle de expediente', module: 'EXPEDIENTES', description: 'Permite ver y descargar documentos del expediente' },
  { id: 'p-12', code: 'shifts.list', name: 'Listar turnos', module: 'TURNOS', description: 'Permite consultar catálogo de turnos' },
  { id: 'p-13', code: 'shifts.create', name: 'Crear turno', module: 'TURNOS', description: 'Permite parametrizar nuevos turnos' },
  { id: 'p-14', code: 'attendance.records.read', name: 'Ver asistencias', module: 'ASISTENCIA', description: 'Permite consultar registros de marcación' },
  { id: 'p-15', code: 'attendance.records.manage', name: 'Gestionar asistencias', module: 'ASISTENCIA', description: 'Permite aprobar sobretiempos y regularizaciones' },
  { id: 'p-16', code: 'leaves.requests.read', name: 'Ver solicitudes de ausencia', module: 'AUSENCIAS', description: 'Permite consultar solicitudes de licencias' },
  { id: 'p-17', code: 'leaves.requests.review', name: 'Aprobar licencias y vacaciones', module: 'AUSENCIAS', description: 'Permite aprobar o rechazar ausencias' },
  { id: 'p-18', code: 'billing.subscription.manage', name: 'Gestionar suscripción', module: 'FACTURACIÓN', description: 'Permite gestionar planes y métodos de pago' }
];

export const MOCK_ROLES: MockRole[] = [
  {
    id: 'role-01',
    name: 'ADMINISTRADOR',
    description: 'Acceso total y configuración global del tenant',
    active: true,
    systemRole: true,
    permissionCount: MOCK_PERMISSIONS.length,
    permissions: [...MOCK_PERMISSIONS],
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z'
  },
  {
    id: 'role-02',
    name: 'GESTOR_RRHH',
    description: 'Gestión completa de personal, expedientes y contratos',
    active: true,
    systemRole: false,
    permissionCount: 10,
    permissions: MOCK_PERMISSIONS.filter(p => ['USUARIOS', 'EXPEDIENTES', 'CONTRATOS', 'AUSENCIAS'].includes(p.module)),
    createdAt: '2026-01-05T08:00:00Z',
    updatedAt: '2026-01-05T08:00:00Z'
  },
  {
    id: 'role-03',
    name: 'SUPERVISOR',
    description: 'Aprobación de asistencias, turnos y vacaciones de su equipo',
    active: true,
    systemRole: false,
    permissionCount: 6,
    permissions: MOCK_PERMISSIONS.filter(p => ['TURNOS', 'ASISTENCIA', 'AUSENCIAS'].includes(p.module)),
    createdAt: '2026-01-10T10:00:00Z',
    updatedAt: '2026-01-10T10:00:00Z'
  },
  {
    id: 'role-04',
    name: 'COLABORADOR',
    description: 'Autoservicio, marcaciones y solicitudes personales',
    active: true,
    systemRole: true,
    permissionCount: 3,
    permissions: MOCK_PERMISSIONS.filter(p => p.code.includes('.read')),
    createdAt: '2026-01-12T12:00:00Z',
    updatedAt: '2026-01-12T12:00:00Z'
  }
];

export const MOCK_CONTRACTS = [
  {
    id: 'ctr-01',
    contractNumber: 'CTR-2026-001',
    employeeId: '9b1deb4d-3b7d-4bad-9bdd-2b0d7b3dcb6d',
    employeeName: 'Carlos Mamani Quispe',
    contractType: 'INDEFINITE',
    status: 'ACTIVE',
    startDate: '2026-01-15',
    endDate: null,
    baseSalary: 6500.0,
    currency: 'BOB',
    departmentName: 'Tecnología',
    positionTitle: 'Desarrollador Senior',
    createdAt: '2026-01-15T08:30:00Z'
  },
  {
    id: 'ctr-02',
    contractNumber: 'CTR-2026-002',
    employeeId: '8a2ceb3c-4a6c-4cbe-8acc-1a0c6a2cba5c',
    employeeName: 'Valeria Rios Salazar',
    contractType: 'FIXED_TERM',
    status: 'ACTIVE',
    startDate: '2026-02-01',
    endDate: '2026-12-31',
    baseSalary: 5200.0,
    currency: 'BOB',
    departmentName: 'Recursos Humanos',
    positionTitle: 'Analista de Talento',
    createdAt: '2026-02-01T09:00:00Z'
  }
];

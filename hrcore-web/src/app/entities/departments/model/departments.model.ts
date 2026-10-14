export type DepartmentStatus = 'ACTIVE' | 'INACTIVE';
export interface DepartmentRequest {
  code: string;
  name: string;
  description: string | null;
  parentDepartmentId: string | null;
  managerEmployeeId: string | null;
  costCenterCode: string | null;
  reason: string | null;
}
export interface Department extends Omit<DepartmentRequest, 'reason'> {
  id: string;
  status: DepartmentStatus;
  createdBy: string;
  updatedBy: string;
  createdAt: string;
  updatedAt: string;
}
export interface DepartmentHistory {
  id: string;
  departmentId: string;
  action: 'CREATED' | 'UPDATED' | 'ACTIVATED' | 'DEACTIVATED' | 'REORGANIZED' | 'MANAGER_CHANGED';
  previousData: Record<string, unknown> | null;
  newData: Record<string, unknown>;
  reason: string | null;
  performedBy: string;
  createdAt: string;
}
export interface DepartmentFilters { name: string; code: string; status: DepartmentStatus | ''; parentDepartmentId: string; }

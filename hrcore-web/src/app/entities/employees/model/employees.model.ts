export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED' | 'INACTIVE';
export type AssignmentType = 'PRIMARY' | 'TEMPORARY' | 'ADDITIONAL';

export interface EmployeeRequest {
  tenantUserId: string | null;
  firstName: string;
  middleName: string | null;
  lastName: string;
  secondLastName: string | null;
  documentType: string;
  documentNumber: string;
  birthDate: string | null;
  gender: string | null;
  nationality: string | null;
  personalEmail: string | null;
  workEmail: string | null;
  phone: string | null;
  mobilePhone: string | null;
  address: string | null;
  hireDate: string;
}

export interface EmployeeSummary {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  employmentStatus: EmploymentStatus;
}

export interface Employee extends EmployeeRequest {
  id: string;
  employeeNumber: string;
  terminationDate: string | null;
  employmentStatus: EmploymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeAssignmentRequest {
  departmentId: string;
  positionId: string;
  supervisorEmployeeId: string | null;
  startDate: string;
  endDate: string | null;
  assignmentType: AssignmentType;
  reason: string | null;
}

export interface EmployeeAssignment extends EmployeeAssignmentRequest {
  id: string;
  employeeId: string;
  createdAt: string;
  updatedAt: string;
  status: 'ACTIVE' | 'CANCELLED';
}

export interface EmployeeOnboardingRequest {
  employee: EmployeeRequest;
  assignment: EmployeeAssignmentRequest;
}

export interface EmployeeOnboardingResponse {
  employee: Employee;
  assignment: EmployeeAssignment;
  employeeFile: { id: string; employeeId: string; fileNumber: string; status: 'ACTIVE' | 'ARCHIVED'; notes?: string | null; createdAt: string; updatedAt: string };
}

export interface EmploymentPeriod {
  id: string;
  employeeId: string;
  periodNumber: number;
  startDate: string;
  endDate: string | null;
  status: EmploymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface EmployeeTermination {
  employeeId: string;
  employmentPeriodId: string;
  effectiveDate: string;
  reason: string;
  performedBy: string | null;
  createdAt: string;
}

export interface RehireEmployeeRequest {
  startDate: string;
  reason: string | null;
  assignment: EmployeeAssignmentRequest | null;
}

export interface RehireEmployeeResponse {
  employee: Employee;
  employmentPeriod: EmploymentPeriod;
  assignment: EmployeeAssignment | null;
}

export interface EmergencyContactRequest {
  fullName: string;
  relationship: string | null;
  phone: string;
  alternativePhone: string | null;
  address: string | null;
  primary: boolean;
}

export interface EmergencyContact extends EmergencyContactRequest {
  id: string;
  employeeId: string;
  createdAt: string;
  updatedAt: string;
}

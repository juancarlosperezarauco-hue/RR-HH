export type EmploymentStatus = 'ACTIVE' | 'ON_LEAVE' | 'SUSPENDED' | 'TERMINATED' | 'INACTIVE';
export type AssignmentType = 'PRIMARY' | 'TEMPORARY' | 'ADDITIONAL';

export interface EmployeeRequest {
  tenantUserId: string | null;
  employeeNumber: string;
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

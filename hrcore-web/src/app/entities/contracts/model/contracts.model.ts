import { PageResponse } from '../../access';

export type ContractType = 'INDEFINITE' | 'FIXED_TERM' | 'SEASONAL' | 'WORK_OR_SERVICE' | 'EVENTUAL' | 'INTERNSHIP' | 'OTHER';
export type ContractStatus = 'DRAFT' | 'PENDING_SIGNATURE' | 'ACTIVE' | 'EXPIRED' | 'TERMINATED' | 'CANCELLED';
export type ContractForm = 'WRITTEN' | 'VERBAL';
export type WorkSchedule = 'FULL_TIME' | 'PART_TIME';
export type WorkModality = 'ONSITE' | 'REMOTE' | 'HYBRID';
export type PaymentFrequency = 'MONTHLY' | 'BIWEEKLY' | 'WEEKLY' | 'DAILY';

export interface ContractRequest {
  employeeId: string;
  employeeAssignmentId: string;
  contractNumber: string;
  contractType: ContractType;
  contractForm: ContractForm;
  workSchedule: WorkSchedule;
  workModality: WorkModality;
  startDate: string;
  endDate: string | null;
  baseSalary: number;
  currency: string;
  paymentFrequency: PaymentFrequency;
  weeklyHours: number | null;
  probationEndDate: string | null;
  terms: string | null;
}

export interface ContractRenewalRequest extends Omit<ContractRequest, 'employeeId'> { reason: string | null; }

export interface EmploymentContract extends ContractRequest {
  id: string;
  status: ContractStatus;
  signedAt: string | null;
  terminationDate: string | null;
  terminationReason: string | null;
  renewedFromContractId: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface ContractHistory {
  id: string;
  contractId: string;
  action: string;
  previousStatus: ContractStatus | null;
  newStatus: ContractStatus | null;
  details: Record<string, unknown>;
  reason: string | null;
  performedBy: string | null;
  createdAt: string;
}

export interface EmployeeOption { id: string; employeeNumber: string; firstName: string; lastName: string; employmentStatus: string; }
export interface AssignmentOption { id: string; employeeId: string; departmentId: string; positionId: string; startDate: string; endDate: string | null; assignmentType: string; }
export interface DepartmentOption { id: string; code: string; name: string; status: string; }
export interface PositionOption { id: string; code: string; name: string; status: string; }
export type ContractPage = PageResponse<EmploymentContract>;

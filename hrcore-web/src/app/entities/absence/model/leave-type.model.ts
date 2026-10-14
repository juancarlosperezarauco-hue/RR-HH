export type LeaveTypeStatus = 'ACTIVE' | 'INACTIVE';

export interface LeaveTypeResponse {
  id: string;               // UUID
  code: string;             // VARCHAR(50) (e.g. VACATION, SICK_LEAVE, UNPAID)
  name: string;             // VARCHAR(150)
  description?: string | null;
  paid?: boolean;
  requiresApproval: boolean;
  requiresAttachment: boolean;
  deductsBalance?: boolean;
  status: LeaveTypeStatus;
  createdAt?: string;
  updatedAt?: string;

  // Backwards compatibility aliases
  tenantId?: string;
  isPaid?: boolean;
  maxDaysPerYear?: number | null;
}

export interface CreateLeaveTypeInput {
  code: string;
  name: string;
  description?: string;
  paid: boolean;
  requiresApproval: boolean;
  requiresAttachment: boolean;
  deductsBalance: boolean;
}

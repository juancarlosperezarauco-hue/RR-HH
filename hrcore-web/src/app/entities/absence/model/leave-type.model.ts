export interface LeaveTypeResponse {
  id: string;               // UUID
  tenantId: string;         // UUID
  code: string;             // VARCHAR(50) (LEAVE_VACATION, LEAVE_MEDICAL, etc.)
  name: string;             // VARCHAR(100)
  isPaid: boolean;
  requiresApproval: boolean;
  requiresAttachment: boolean;
  maxDaysPerYear?: number | null;
  description?: string | null;
  status: 'ACTIVE' | 'INACTIVE';
}

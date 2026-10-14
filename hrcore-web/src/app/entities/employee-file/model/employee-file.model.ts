export type FileStatus = 'ACTIVE' | 'ARCHIVED';

export interface EmployeeFileResponse {
  id: string;              // UUID
  employeeId: string;      // UUID
  fileNumber: string;      // VARCHAR(80)
  status: FileStatus;      // ACTIVE | ARCHIVED
  notes?: string | null;
  createdAt: string;       // ISO Instant
  updatedAt: string;       // ISO Instant
}

export interface CreateFileRequest {
  employeeId: string;
  fileNumber: string;
  notes?: string;
}

export interface UpdateFileRequest {
  fileNumber: string;
  notes?: string;
  status: FileStatus;
}

export interface EmployeeFileFilter {
  employeeId?: string;
  fileNumber?: string;
  status?: FileStatus;
}

export interface EmployeeSummaryResponse {
  id: string;
  employeeNumber: string;
  firstName: string;
  lastName: string;
  employmentStatus: string;
}

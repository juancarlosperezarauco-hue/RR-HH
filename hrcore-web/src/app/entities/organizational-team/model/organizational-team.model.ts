export interface TeamEmployee {
  employeeId: string;
  employeeNumber: string | null;
  fullName: string;
  departmentId: string | null;
  positionId: string | null;
  employmentStatus: string;
}

export interface OrganizationChartNode {
  employeeId: string;
  employeeNumber: string;
  fullName: string;
  departmentId: string | null;
  departmentName: string | null;
  positionId: string | null;
  positionName: string | null;
  supervisorEmployeeId: string | null;
  children: OrganizationChartNode[];
}

export interface OrganizationChart {
  asOfDate: string;
  totalEmployees: number;
  roots: OrganizationChartNode[];
}

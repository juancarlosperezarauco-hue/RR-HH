export const ORGANIZATIONAL_LEVELS = ['EXECUTIVE', 'MANAGEMENT', 'SUPERVISORY', 'PROFESSIONAL', 'TECHNICAL', 'OPERATIONAL'] as const;
export const REQUIREMENT_TYPES = ['EDUCATION', 'EXPERIENCE', 'SKILL', 'CERTIFICATION', 'OTHER'] as const;
export type OrganizationalLevel = typeof ORGANIZATIONAL_LEVELS[number];
export type RequirementType = typeof REQUIREMENT_TYPES[number];
export type PositionStatus = 'ACTIVE' | 'INACTIVE';
export interface PositionRequest {
  code: string; name: string; description: string | null; responsibilities: string | null;
  organizationalLevel: OrganizationalLevel | null;
  // Decimal text in the frontend; the service sends these fields as JSON numbers.
  salaryMin: string | null; salaryMax: string | null; currency: string | null;
}
export interface Position extends PositionRequest { id: string; status: PositionStatus; createdAt: string; updatedAt: string; }
export interface PositionRequirementRequest {
  requirementType: RequirementType; description: string; mandatory: boolean; minimumYearsExperience: number | null;
}
export interface PositionRequirement extends PositionRequirementRequest { id: string; positionId: string; createdAt: string; updatedAt: string; }
export interface PositionFilters { name: string; code: string; status: PositionStatus | ''; organizationalLevel: OrganizationalLevel | ''; }

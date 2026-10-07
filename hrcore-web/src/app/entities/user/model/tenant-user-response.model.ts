export interface TenantUserResponse {
  id: number;
  email: string;
  firstName: string;
  lastName: string;
  active: boolean;
  status?: 'ACTIVE' | 'INACTIVE' | 'PENDING' | string;
}

export interface PageResponse<T> {
  content: T[];
  pageable: {
    pageNumber: number;
    pageSize: number;
    sort?: unknown;
    offset?: number;
    unpaged?: boolean;
    paged?: boolean;
  };
  totalPages: number;
  totalElements: number;
  last: boolean;
  size: number;
  number: number;
  sort?: unknown;
  numberOfElements: number;
  first: boolean;
  empty: boolean;
}

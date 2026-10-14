export interface SystemPermissionResponse {
  code: string;
  module: string;
  description: string;
  scope: 'TENANT' | 'PLATFORM';
  customRoleAssignable: boolean;
}

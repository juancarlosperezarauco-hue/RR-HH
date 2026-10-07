export interface LoginRequest {
  email: string;
  password?: string;
  tenantSlug?: string; // Incluido para poder mandarlo en el header desde el servicio
}

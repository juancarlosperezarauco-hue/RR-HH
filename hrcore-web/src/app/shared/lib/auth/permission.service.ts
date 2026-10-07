import { inject, Injectable } from '@angular/core';
import { AuthStorageService } from '../storage/auth-storage.service';

interface JwtPayload {
  sub: string;
  tenant: string;
  roles: string[];
  permissions: string[];
  token_type: string;
  iat: number;
  exp: number;
}

@Injectable({
  providedIn: 'root'
})
export class PermissionService {
  private readonly authStorage = inject(AuthStorageService);

  // Cache
  private cachedToken: string | null = null;
  private cachedPermissions: Set<string> = new Set();
  private cachedRoles: Set<string> = new Set();

  /**
   * Verifica si el usuario autenticado tiene un permiso específico.
   * Decodifica el JWT del localStorage y busca en el claim "permissions".
   */
  hasPermission(permissionCode: string): boolean {
    this.refreshCacheIfNeeded();
    return this.cachedPermissions.has(permissionCode);
  }

  /**
   * Verifica si el usuario tiene AL MENOS UNO de los permisos dados.
   */
  hasAnyPermission(...permissionCodes: string[]): boolean {
    this.refreshCacheIfNeeded();
    return permissionCodes.some(code => this.cachedPermissions.has(code));
  }

  /**
   * Verifica si el usuario tiene un rol específico (ej. "ADMIN", "OWNER_ADMIN").
   */
  hasRole(roleName: string): boolean {
    this.refreshCacheIfNeeded();
    return this.cachedRoles.has(roleName.toUpperCase());
  }

  /**
   * Retorna todos los permisos del usuario autenticado.
   */
  getPermissions(): string[] {
    this.refreshCacheIfNeeded();
    return Array.from(this.cachedPermissions);
  }

  /**
   * Retorna todos los roles del usuario autenticado.
   */
  getRoles(): string[] {
    this.refreshCacheIfNeeded();
    return Array.from(this.cachedRoles);
  }

  /**
   * Fuerza la re-lectura del JWT (útil después de un login o refresh).
   */
  invalidateCache(): void {
    this.cachedToken = null;
    this.cachedPermissions.clear();
    this.cachedRoles.clear();
  }

  // --- Internos ---

  private refreshCacheIfNeeded(): void {
    const currentToken = this.authStorage.getToken();

    // Si no hay token, limpiar
    if (!currentToken) {
      if (this.cachedToken !== null) {
        this.invalidateCache();
      }
      return;
    }

    // Si el token no cambió, usar cache
    if (currentToken === this.cachedToken) {
      return;
    }

    // Token nuevo o diferente → parsear
    this.cachedToken = currentToken;
    this.cachedPermissions.clear();
    this.cachedRoles.clear();

    try {
      const payload = this.decodeJwtPayload(currentToken);

      if (Array.isArray(payload.permissions)) {
        payload.permissions.forEach(p => this.cachedPermissions.add(p));
      }

      if (Array.isArray(payload.roles)) {
        payload.roles.forEach(r => this.cachedRoles.add(r.toUpperCase()));
      }
    } catch (err) {
      console.error('[PermissionService] Error al decodificar el JWT:', err);
      this.invalidateCache();
    }
  }

  private decodeJwtPayload(token: string): JwtPayload {
    const parts = token.split('.');
    if (parts.length !== 3) {
      throw new Error('JWT inválido: no tiene 3 partes');
    }

    // El payload es la segunda parte (index 1)
    const payloadBase64Url = parts[1];

    // Base64URL → Base64 estándar
    const payloadBase64 = payloadBase64Url
      .replace(/-/g, '+')
      .replace(/_/g, '/');

    // Decodificar
    const payloadJson = atob(payloadBase64);
    return JSON.parse(payloadJson) as JwtPayload;
  }
}

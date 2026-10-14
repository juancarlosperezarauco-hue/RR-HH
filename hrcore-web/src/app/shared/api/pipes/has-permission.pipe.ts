import { inject, Pipe, PipeTransform } from '@angular/core';
import { PermissionService } from '../../lib/auth/permission.service';

/**
 * Pipe reactivo: la fuente de verdad es la proyección actual de /me.
 *
 * Uso:
 *   *ngIf="'accounts.create' | hasPermission"
 *   *ngIf="'transactions.create.deposit' | hasPermission"
 */
@Pipe({
  name: 'hasPermission',
  standalone: true,
  pure: false
})
export class HasPermissionPipe implements PipeTransform {
  private readonly permissionService = inject(PermissionService);

  transform(permissionCode: string): boolean {
    return this.permissionService.hasPermission(permissionCode);
  }
}

/**
 * Pipe puro para verificar si el usuario tiene AL MENOS UNO de los permisos dados.
 *
 * Uso:
 *   *ngIf="['transactions.create.deposit', 'transactions.create.withdrawal'] | hasAnyPermission"
 */
@Pipe({
  name: 'hasAnyPermission',
  standalone: true,
  pure: false
})
export class HasAnyPermissionPipe implements PipeTransform {
  private readonly permissionService = inject(PermissionService);

  transform(permissionCodes: string[]): boolean {
    return this.permissionService.hasAnyPermission(...permissionCodes);
  }
}

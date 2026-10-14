import { Component, EventEmitter, Input, Output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantRoleResponse } from '../../../../entities/access';

@Component({
  selector: 'app-role-table',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './role-table.component.html'
})
export class RoleTableComponent {
  @Input() roles: TenantRoleResponse[] = [];
  @Input() actions: RoleActions = { canCreate: false, canEdit: false, canActivate: false, canDeactivate: false };
  @Output() edit = new EventEmitter<TenantRoleResponse>();
  @Output() toggleStatus = new EventEmitter<{id: string, currentStatus: boolean}>();

  onEdit(role: TenantRoleResponse): void {
    this.edit.emit(role);
  }

  onToggleStatus(role: TenantRoleResponse): void {
    this.toggleStatus.emit({ id: role.id, currentStatus: role.active });
  }
}

export interface RoleActions { canCreate: boolean; canEdit: boolean; canActivate: boolean; canDeactivate: boolean; }

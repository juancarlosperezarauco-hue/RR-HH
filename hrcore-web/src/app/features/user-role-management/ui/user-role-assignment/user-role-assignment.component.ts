import { Component, EventEmitter, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { TenantRoleResponse } from '../../../../entities/access';
import { TenantUserResponse } from '../../../../entities/user';

@Component({
  selector: 'app-user-role-assignment',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './user-role-assignment.component.html'
})
export class UserRoleAssignmentComponent implements OnChanges {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() user: TenantUserResponse | null = null;
  @Input() availableRoles: TenantRoleResponse[] = [];
  @Input() assignedRoles: TenantRoleResponse[] = [];
  
  @Output() submitRoles = new EventEmitter<string[]>();
  @Output() cancel = new EventEmitter<void>();

  // Estado local para los checkboxes
  selectedRoleIds: Set<string> = new Set<string>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['assignedRoles']) {
      this.selectedRoleIds = new Set(this.assignedRoles.map(r => r.id));
    }
  }

  toggleRole(roleId: string, event: Event): void {
    const isChecked = (event.target as HTMLInputElement).checked;
    if (isChecked) {
      this.selectedRoleIds.add(roleId);
    } else {
      this.selectedRoleIds.delete(roleId);
    }
  }

  hasRole(roleId: string): boolean {
    return this.selectedRoleIds.has(roleId);
  }

  onSubmit(): void {
    if (this.status !== 'loading') {
      this.submitRoles.emit(Array.from(this.selectedRoleIds));
    }
  }

  onCancel(): void {
    if (this.status !== 'loading') {
      this.cancel.emit();
    }
  }
}

import { inject, Injectable, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { AbsenceService, LeaveBalanceResponse } from '../../../entities/absence';

@Injectable({ providedIn: 'root' })
export class VacationBalanceUseCase {
  private readonly absenceService = inject(AbsenceService);

  readonly balance = signal<LeaveBalanceResponse | null>(null);
  readonly loading = signal<boolean>(false);
  readonly error = signal<string | null>(null);

  /**
   * Escala de Vacaciones LGT Bolivia según años de antigüedad
   * - 1 a 5 años: 15 días hábiles
   * - 5 a 10 años: 20 días hábiles
   * - Más de 10 años: 30 días hábiles
   */
  static getBolivianEntitledDays(yearsOfService: number): number {
    if (yearsOfService < 1) return 0;
    if (yearsOfService < 5) return 15;
    if (yearsOfService < 10) return 20;
    return 30;
  }

  async loadBalance(employeeId: string): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(this.absenceService.getEmployeeBalance(employeeId));
      this.balance.set(res.data);
    } catch (err: any) {
      this.error.set(err?.error?.message || 'Error al obtener el saldo de vacaciones');
    } finally {
      this.loading.set(false);
    }
  }
}

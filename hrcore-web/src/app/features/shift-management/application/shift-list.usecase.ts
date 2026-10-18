import { inject, Injectable, signal, computed } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ShiftService, ShiftResponse } from '../../../entities/shift';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Injectable({ providedIn: 'root' })
export class ShiftListUseCase {
  private readonly shiftService = inject(ShiftService);
  private readonly toastService = inject(ToastService);

  readonly shifts = signal<ShiftResponse[]>([]);
  readonly loading = signal(false);
  readonly statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly searchQuery = signal('');
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);

  // Filtrado reactivo en cliente (soporta búsqueda instantánea y filtro de estado)
  readonly filteredShifts = computed(() => {
    const query = this.searchQuery().toLowerCase().trim();
    const status = this.statusFilter();
    let list = this.shifts();

    if (status !== 'ALL') {
      list = list.filter((s) => s.status === status);
    }

    if (query) {
      list = list.filter(
        (s) =>
          s.name.toLowerCase().includes(query) ||
          s.code.toLowerCase().includes(query) ||
          s.shiftType.toLowerCase().includes(query)
      );
    }

    return list;
  });

  // Métricas reactivas (KPIs del módulo)
  readonly kpis = computed(() => {
    const all = this.shifts();
    const total = all.length;
    const active = all.filter((s) => s.status === 'ACTIVE').length;
    const night = all.filter((s) => s.shiftType === 'NIGHT' || s.isOvernight).length;
    const continuous = all.filter((s) => s.shiftType === 'CONTINUOUS').length;
    return { total, active, night, continuous };
  });

  async loadShifts(): Promise<void> {
    this.loading.set(true);
    try {
      const res = await firstValueFrom(
        this.shiftService.listShifts(this.statusFilter(), this.currentPage(), this.pageSize())
      );
      const shifts = await Promise.all(res.data.content.map(async (shift) => {
        try {
          const breaks = await firstValueFrom(this.shiftService.listBreaks(shift.id, 0, 100));
          const unpaidMinutes = breaks.data.content
            .filter((item) => !item.paid)
            .reduce((total, item) => total + item.durationMinutes, 0);
          return { ...shift, breakMinutes: unpaidMinutes };
        } catch {
          return shift;
        }
      }));
      this.shifts.set(shifts);
      this.totalElements.set(res.data.totalElements);
      this.totalPages.set(res.data.totalPages);
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      this.toastService.error(errorResponse?.error?.message || 'Error al cargar el catálogo de turnos');
    } finally {
      this.loading.set(false);
    }
  }

  setStatusFilter(status: 'ALL' | 'ACTIVE' | 'INACTIVE'): void {
    this.statusFilter.set(status);
    this.currentPage.set(0);
    this.loadShifts();
  }

  setSearchQuery(query: string): void {
    this.searchQuery.set(query);
  }

  setPage(page: number): void {
    this.currentPage.set(page);
    this.loadShifts();
  }

  async toggleStatus(shift: ShiftResponse): Promise<void> {
    const nextStatus = shift.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    try {
      await firstValueFrom(this.shiftService.toggleShiftStatus(shift.id, nextStatus));
      this.toastService.success(`Turno ${shift.code} marcado como ${nextStatus === 'ACTIVE' ? 'Activo' : 'Inactivo'}`);
      this.shifts.update((list) =>
        list.map((s) => (s.id === shift.id ? { ...s, status: nextStatus } : s))
      );
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      this.toastService.error(errorResponse?.error?.message || 'No se pudo cambiar el estado del turno');
    }
  }
}

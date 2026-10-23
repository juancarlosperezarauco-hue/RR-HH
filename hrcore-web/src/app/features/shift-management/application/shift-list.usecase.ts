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
  readonly error = signal<string | null>(null);
  readonly statusFilter = signal<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  readonly searchQuery = signal('');
  readonly currentPage = signal(0);
  readonly pageSize = signal(10);
  readonly totalElements = signal(0);
  readonly totalPages = signal(1);
  readonly kpiShifts = signal<ShiftResponse[]>([]);

  // El estado y la búsqueda se resuelven en el backend para que la paginación
  // y los resultados sean consistentes con el total real del catálogo.
  readonly filteredShifts = computed(() => {
    return this.shifts();
  });

  // Métricas reactivas (KPIs del módulo)
  readonly kpis = computed(() => {
    const all = this.kpiShifts();
    const total = all.length;
    const active = all.filter((s) => s.status === 'ACTIVE').length;
    const night = all.filter((s) => s.shiftType === 'NIGHT' || s.isOvernight).length;
    const continuous = all.filter((s) => s.shiftType === 'CONTINUOUS').length;
    return { total, active, night, continuous };
  });

  async loadShifts(): Promise<void> {
    this.loading.set(true);
    this.error.set(null);
    try {
      const res = await firstValueFrom(
        this.shiftService.listShifts(
          this.statusFilter(),
          this.currentPage(),
          this.pageSize(),
          undefined,
          this.searchQuery().trim() || undefined
        )
      );
      const shifts = res.data.content.map((shift) => ({
        ...shift,
        breakMinutes: shift.breakMinutes ?? 0
      }));
      this.shifts.set(shifts);
      this.totalElements.set(res.data.totalElements);
      this.totalPages.set(res.data.totalPages);
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      const message = errorResponse?.error?.message || 'Error al cargar el catálogo de turnos';
      this.error.set(message);
      this.toastService.error(message);
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
    this.currentPage.set(0);
    void this.loadShifts();
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
      void this.loadKpis();
    } catch (err: unknown) {
      const errorResponse = err as { error?: { message?: string } };
      this.toastService.error(errorResponse?.error?.message || 'No se pudo cambiar el estado del turno');
    }
  }

  async loadKpis(): Promise<void> {
    try {
      const first = await firstValueFrom(this.shiftService.listShifts('ALL', 0, 100));
      const all = [...first.data.content.map((shift) => this.toEnrichedShift(shift))];
      const remainingPages = Array.from({ length: Math.max(0, first.data.totalPages - 1) }, (_, index) => index + 1);
      const rest = await Promise.all(remainingPages.map((page) =>
        firstValueFrom(this.shiftService.listShifts('ALL', page, 100))
      ));
      rest.forEach((response) => all.push(...response.data.content.map((shift) => this.toEnrichedShift(shift))));
      this.kpiShifts.set(all);
    } catch {
      // Los KPIs no deben borrar el catálogo si una consulta secundaria falla.
    }
  }

  private toEnrichedShift(shift: ShiftResponse): ShiftResponse {
    return { ...shift, breakMinutes: shift.breakMinutes ?? 0 };
  }
}

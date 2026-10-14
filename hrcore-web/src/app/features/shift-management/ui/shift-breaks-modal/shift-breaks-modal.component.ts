import { CommonModule } from '@angular/common';
import { Component, EventEmitter, inject, Input, OnChanges, Output, SimpleChanges } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom } from 'rxjs';
import { ShiftBreakResponse, ShiftResponse, ShiftService } from '../../../../entities/shift';
import { ToastService } from '../../../../shared/ui/toast/toast.service';

@Component({
  selector: 'app-shift-breaks-modal',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div *ngIf="isOpen && shift" class="fixed inset-0 z-50 flex items-center justify-center bg-black/60 p-4 backdrop-blur-sm">
      <div class="max-h-[92vh] w-full max-w-2xl overflow-y-auto rounded-[24px] border border-border bg-card p-6 shadow-2xl">
        <div class="flex items-start justify-between border-b border-border pb-4">
          <div>
            <h3 class="text-xl font-black text-accent-foreground">Descansos del turno</h3>
            <p class="text-sm text-muted-foreground">{{ shift.name }} · {{ shift.code }}</p>
          </div>
          <button type="button" (click)="close()" class="rounded-full p-2 hover:bg-muted"><lucide-icon name="x" [size]="18"></lucide-icon></button>
        </div>

        <div class="mt-5 space-y-3">
          <div *ngIf="loading" class="rounded-xl border border-dashed p-5 text-center text-sm text-muted-foreground">Cargando descansos...</div>
          <div *ngFor="let item of breaks" class="flex flex-col gap-3 rounded-2xl border border-border p-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <p class="font-black text-accent-foreground">{{ item.name }}</p>
              <p class="text-sm text-muted-foreground">{{ formatTime(item.startTime) }}–{{ formatTime(item.endTime) }} · {{ item.durationMinutes }} min · {{ item.paid ? 'Remunerado' : 'No remunerado' }}</p>
            </div>
            <div class="flex gap-2">
              <button type="button" (click)="edit(item)" class="rounded-full border border-border px-3 py-1.5 text-xs font-bold text-primary">Editar</button>
              <button type="button" (click)="remove(item)" class="rounded-full border border-red-200 px-3 py-1.5 text-xs font-bold text-red-700">Eliminar</button>
            </div>
          </div>
          <p *ngIf="!loading && !breaks.length" class="rounded-xl border border-dashed p-5 text-center text-sm text-muted-foreground">El turno todavía no tiene descansos registrados.</p>
        </div>

        <form (ngSubmit)="save()" class="mt-6 rounded-2xl bg-muted/40 p-4">
          <h4 class="mb-4 font-black">{{ editingId ? 'Editar descanso' : 'Nuevo descanso' }}</h4>
          <div class="grid gap-4 sm:grid-cols-2">
            <label class="space-y-1 sm:col-span-2"><span class="text-xs font-bold">Nombre *</span><input name="breakName" [(ngModel)]="name" required maxlength="100" placeholder="Almuerzo" class="h-11 w-full rounded-xl border border-border bg-background px-3"></label>
            <label class="space-y-1"><span class="text-xs font-bold">Hora de inicio *</span><input name="breakStart" [(ngModel)]="startTime" type="time" required class="h-11 w-full rounded-xl border border-border bg-background px-3"></label>
            <label class="space-y-1"><span class="text-xs font-bold">Hora de fin *</span><input name="breakEnd" [(ngModel)]="endTime" type="time" required class="h-11 w-full rounded-xl border border-border bg-background px-3"></label>
            <label class="flex items-center gap-3 sm:col-span-2"><input name="breakPaid" [(ngModel)]="paid" type="checkbox" class="h-4 w-4"><span class="text-sm font-bold">Descanso remunerado</span></label>
          </div>
          <p class="mt-3 text-xs text-muted-foreground">Duración calculada: <strong>{{ durationMinutes }} minutos</strong></p>
          <div class="mt-4 flex justify-end gap-2">
            <button *ngIf="editingId" type="button" (click)="resetForm()" class="rounded-full border border-border px-4 py-2 text-sm font-bold">Cancelar edición</button>
            <button type="submit" [disabled]="saving || durationMinutes <= 0" class="rounded-full bg-primary px-5 py-2 text-sm font-black text-primary-foreground disabled:opacity-50">{{ saving ? 'Guardando...' : 'Guardar descanso' }}</button>
          </div>
        </form>
      </div>
    </div>
  `
})
export class ShiftBreaksModalComponent implements OnChanges {
  @Input() isOpen = false;
  @Input() shift: ShiftResponse | null = null;
  @Output() closeModal = new EventEmitter<void>();
  @Output() changed = new EventEmitter<void>();

  private readonly service = inject(ShiftService);
  private readonly toast = inject(ToastService);

  breaks: ShiftBreakResponse[] = [];
  loading = false;
  saving = false;
  editingId: string | null = null;
  name = 'Almuerzo';
  startTime = '12:00';
  endTime = '12:45';
  paid = false;

  ngOnChanges(changes: SimpleChanges): void {
    if ((changes['isOpen'] || changes['shift']) && this.isOpen && this.shift) this.load();
  }

  get durationMinutes(): number {
    if (!this.startTime || !this.endTime) return 0;
    const [sh, sm] = this.startTime.split(':').map(Number);
    const [eh, em] = this.endTime.split(':').map(Number);
    let duration = eh * 60 + em - (sh * 60 + sm);
    if (duration <= 0) duration += 1440;
    return duration;
  }

  async load(): Promise<void> {
    if (!this.shift) return;
    this.loading = true;
    try {
      const response = await firstValueFrom(this.service.listBreaks(this.shift.id, 0, 100));
      this.breaks = response.data.content;
    } catch (error: unknown) {
      this.breaks = [];
      this.handleError(error, 'No se pudieron cargar los descansos.');
    } finally {
      this.loading = false;
    }
  }

  edit(item: ShiftBreakResponse): void {
    this.editingId = item.id;
    this.name = item.name;
    this.startTime = this.formatTime(item.startTime);
    this.endTime = this.formatTime(item.endTime);
    this.paid = item.paid;
  }

  async save(): Promise<void> {
    if (!this.shift || !this.name.trim() || this.durationMinutes <= 0) return;
    this.saving = true;
    const request = {
      name: this.name.trim(),
      startTime: this.withSeconds(this.startTime),
      endTime: this.withSeconds(this.endTime),
      durationMinutes: this.durationMinutes,
      paid: this.paid
    };
    try {
      if (this.editingId) await firstValueFrom(this.service.updateBreak(this.shift.id, this.editingId, request));
      else await firstValueFrom(this.service.createBreak(this.shift.id, request));
      this.toast.success('Descanso guardado correctamente.');
      this.resetForm();
      await this.load();
      this.changed.emit();
    } catch (error: unknown) {
      this.handleError(error, 'No se pudo guardar el descanso.');
    } finally {
      this.saving = false;
    }
  }

  async remove(item: ShiftBreakResponse): Promise<void> {
    if (!this.shift || !window.confirm(`¿Eliminar el descanso ${item.name}?`)) return;
    try {
      await firstValueFrom(this.service.deleteBreak(this.shift.id, item.id));
      this.toast.success('Descanso eliminado correctamente.');
      await this.load();
      this.changed.emit();
    } catch (error: unknown) {
      this.handleError(error, 'No se pudo eliminar el descanso.');
    }
  }

  close(): void { this.closeModal.emit(); }
  formatTime(value: string): string { return value?.substring(0, 5) || ''; }
  resetForm(): void { this.editingId = null; this.name = 'Almuerzo'; this.startTime = '12:00'; this.endTime = '12:45'; this.paid = false; }
  private withSeconds(value: string): string { return value.length === 5 ? `${value}:00` : value; }
  private handleError(error: unknown, fallback: string): void {
    const response = error as { error?: { message?: string } };
    this.toast.error(response.error?.message || fallback);
  }
}

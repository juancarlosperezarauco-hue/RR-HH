import { CommonModule } from '@angular/common';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom, timeout } from 'rxjs';
import {
  CreateBackupRequest,
  PageResponse,
  PlatformBackup,
  PlatformService,
  RestoreBackupRequest
} from '../../entities/platform/api/platform.service';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { HasPermissionPipe } from '../../shared/api';
import { ToastService } from '../../shared/ui/toast/toast.service';

@Component({
  selector: 'app-backups-page',
  standalone: true,
  imports: [
    CommonModule,
    ReactiveFormsModule,
    LucideAngularModule,
    PlatformPaginationComponent,
    HasPermissionPipe
  ],
  template: `
    <section class="space-y-6">
      <div class="rounded-[28px] border border-[#C8E6C9] bg-white p-6 shadow-sm">
        <div class="flex flex-col gap-4 sm:flex-row sm:items-center sm:justify-between">
          <div class="flex items-start gap-4">
            <div class="rounded-2xl bg-[#F1F8E9] p-3 text-[#2E7D32]">
              <lucide-icon name="archive" class="h-6 w-6"></lucide-icon>
            </div>
            <div>
              <h1 class="text-2xl font-black tracking-tight text-[#1B5E20]">Respaldos</h1>
              <p class="mt-1 text-sm text-[#6B7D6C]">
                Crea respaldos del tenant actual, descarga artefactos y restaura backups completados.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              *ngIf="'backups.create' | hasPermission"
              type="button"
              (click)="openCreateModal()"
              title="Nuevo"
              class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-full bg-[#2E7D32] px-5 py-2.5 text-sm font-semibold text-white transition-colors hover:bg-[#256428]">
              <lucide-icon name="arrow-down-to-line" class="h-4 w-4"></lucide-icon>
              Nuevo respaldo
            </button>

            <button
              *ngIf="'backups.restore' | hasPermission"
              type="button"
              (click)="openRestoreFromFileModal()"
              title="Restaurar"
              class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-full border border-[#DDEED8] bg-white px-5 py-2.5 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="upload" class="h-4 w-4"></lucide-icon>
              Restaurar desde archivo
            </button>

            <button
              type="button"
              (click)="reload()"
              [disabled]="loading()"
              title="Actualizar"
              class="inline-flex cursor-pointer items-center justify-center gap-2 rounded-full border border-[#DDEED8] bg-white px-5 py-2.5 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9] disabled:cursor-not-allowed disabled:opacity-60">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Actualizar
            </button>
          </div>
        </div>
      </div>

      <section class="grid gap-4 md:grid-cols-4">
        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#6B7D6C]">Total</p>
          <p class="mt-2 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
        </div>

        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#6B7D6C]">Completados</p>
          <p class="mt-2 text-3xl font-black text-[#1B5E20]">{{ stats().completed }}</p>
        </div>

        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#6B7D6C]">En proceso</p>
          <p class="mt-2 text-3xl font-black text-[#1B5E20]">{{ stats().running }}</p>
        </div>

        <div class="rounded-[24px] border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#6B7D6C]">Fallidos</p>
          <p class="mt-2 text-3xl font-black text-[#1B5E20]">{{ stats().failed }}</p>
        </div>
      </section>

      <section class="rounded-[28px] border border-[#C8E6C9] bg-white shadow-sm">
        <div class="flex items-center justify-between border-b border-[#E8F2E2] p-5">
          <div>
            <h2 class="text-lg font-black text-[#1B5E20]">Historial de respaldos</h2>
            <p class="mt-1 text-sm text-[#6B7D6C]">Backups y restores del tenant actual.</p>
          </div>
        </div>

        @if (loading()) {
          <div class="p-8 text-center">
            <div class="mx-auto h-8 w-8 animate-spin rounded-full border-4 border-[#C8E6C9] border-t-[#2E7D32]"></div>
            <p class="mt-4 text-sm font-semibold text-[#1B5E20]">Cargando respaldos...</p>
          </div>
        } @else if (error()) {
          <div class="p-8 text-center">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-red-50 text-red-700">
              <lucide-icon name="alert-circle" class="h-6 w-6"></lucide-icon>
            </div>
            <p class="mt-4 text-sm font-bold text-red-700">{{ error() }}</p>
            <button
              type="button"
              (click)="reload()"
              class="mt-4 rounded-full bg-[#2E7D32] px-4 py-2 text-sm font-semibold text-white">
              Reintentar
            </button>
          </div>
        } @else if (backups().length === 0) {
          <div class="p-10 text-center">
            <div class="mx-auto flex h-12 w-12 items-center justify-center rounded-2xl bg-[#F1F8E9] text-[#2E7D32]">
              <lucide-icon name="archive" class="h-6 w-6"></lucide-icon>
            </div>
            <p class="mt-4 text-sm font-bold text-[#1B5E20]">Aún no hay respaldos</p>
            <p class="mt-1 text-sm text-[#6B7D6C]">Crea el primer respaldo del tenant actual.</p>
          </div>
        } @else {
          <div class="overflow-x-auto">
            <table class="min-w-full divide-y divide-[#E8F2E2]">
              <thead class="bg-[#FAFCF8]">
                <tr>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Operación</th>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Estado</th>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Archivo</th>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Tamaño</th>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Solicitado por</th>
                  <th class="px-4 py-3 text-left text-xs font-bold uppercase tracking-wide text-[#567157]">Fecha</th>
                  <th class="px-4 py-3 text-right text-xs font-bold uppercase tracking-wide text-[#567157]">Acciones</th>
                </tr>
              </thead>

              <tbody class="divide-y divide-[#E8F2E2] bg-white">
                @for (backup of backups(); track backup.id) {
                  <tr class="hover:bg-[#FAFCF8]">
                    <td class="px-4 py-4 text-sm">
                      <div class="font-bold text-[#1B5E20]">{{ operationLabel(backup.operationType) }}</div>
                      <div class="text-xs text-[#6B7D6C]">{{ backup.scope }}</div>
                    </td>

                    <td class="px-4 py-4">
                      <span
                        class="inline-flex rounded-full border px-2.5 py-1 text-xs font-bold"
                        [ngClass]="statusClass(backup.status)">
                        {{ statusLabel(backup.status) }}
                      </span>
                    </td>

                    <td class="px-4 py-4 text-sm text-[#1B5E20]">
                      <div class="max-w-[260px] truncate font-semibold">{{ backup.fileName || 'N/A' }}</div>
                      <div class="text-xs text-[#6B7D6C]">{{ backup.format || 'N/A' }}</div>
                    </td>

                    <td class="px-4 py-4 text-sm text-[#6B7D6C]">
                      {{ formatBytes(backup.sizeBytes) }}
                    </td>

                    <td class="px-4 py-4 text-sm text-[#6B7D6C]">
                      {{ backup.requestedBy || 'SYSTEM' }}
                    </td>

                    <td class="px-4 py-4 text-sm text-[#6B7D6C]">
                      {{ formatDate(backup.createdAt) }}
                    </td>

                    <td class="px-4 py-4 text-right">
                      <div class="inline-flex items-center gap-2">
                        <button
                          *ngIf="'backups.detail' | hasPermission"
                          type="button"
                          (click)="openDetail(backup)"
                          title="Detalle"
                          class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                          <lucide-icon name="eye" class="h-4 w-4"></lucide-icon>
                        </button>
                        <button
                          *ngIf="'backups.download' | hasPermission"
                          type="button"
                          (click)="download(backup)"
                          title="Descargar"
                          class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                          <lucide-icon name="arrow-down-to-line" class="h-4 w-4"></lucide-icon>
                        </button>
                        <button
                          *ngIf="backup.status === 'COMPLETED' && ('backups.restore' | hasPermission)"
                          type="button"
                          (click)="openRestoreModal(backup)"
                          title="Restaurar"
                          class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                          <lucide-icon name="rotate-ccw" class="h-4 w-4"></lucide-icon>
                        </button>
                      </div>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          </div>
        }

        @if (backupPage(); as page) {
          <div class="mt-4">
            <app-platform-pagination
              [currentPage]="page.number"
              [totalPages]="page.totalPages"
              [totalElements]="page.totalElements"
              [isLoading]="loading() || saving()"
              (pageChange)="changePage($event)">
            </app-platform-pagination>
          </div>
        }
      </section>

      @if (createModalOpen()) {
        <div class="app-modal-overlay" (click)="closeCreateModal()">
          <div class="app-modal-panel app-modal-panel-sm" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Nuevo respaldo</h2>
                <p class="app-modal-subtitle">Crea un respaldo del tenant actual.</p>
              </div>
              <button type="button" (click)="closeCreateModal()" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <form class="mt-6 grid gap-4" [formGroup]="createForm" (ngSubmit)="submitCreate()">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 text-sm text-[#1B5E20]">
                Este respaldo se crea sobre el tenant actual. Puedes agregar un motivo opcional.
              </div>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Motivo</span>
                <textarea formControlName="reason" rows="3" class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]" placeholder="Opcional"></textarea>
              </label>

              <div class="app-modal-footer">
                <button type="button" (click)="closeCreateModal()" class="cursor-pointer rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  Cancelar
                </button>
                <button type="submit" [disabled]="saving()" class="cursor-pointer rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-60">
                  {{ saving() ? 'Procesando...' : 'Crear respaldo' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (detailModalOpen() && selectedBackup(); as backup) {
        <div class="app-modal-overlay" (click)="closeDetail()">
          <div class="app-modal-panel" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Detalle del respaldo</h2>
                <p class="app-modal-subtitle">Información operativa y trazabilidad.</p>
              </div>
              <button type="button" (click)="closeDetail()" title="Cerrar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <div class="mt-6 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">ID</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.id }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Estado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ statusLabel(backup.status) }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Scope</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.scope }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Archivo</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.fileName || 'N/A' }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Formato</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.format || 'N/A' }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Creado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ formatDate(backup.createdAt) }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Actualizado</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ formatDate(backup.updatedAt) }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Solicitado por</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.requestedBy || 'N/A' }}</p>
              </div>
            </div>

            <div class="mt-4 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
              <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Checksum</p>
              <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.checksumSha256 || 'N/A' }}</p>
            </div>

            <div class="app-modal-footer">
              <button type="button" (click)="closeDetail()" class="cursor-pointer rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                Cerrar
              </button>
            </div>
          </div>
        </div>
      }

      @if (restoreModalOpen() && selectedBackup(); as backup) {
        <div class="app-modal-overlay" (click)="closeRestoreModal()">
          <div class="app-modal-panel app-modal-panel-sm" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Restaurar respaldo</h2>
                <p class="app-modal-subtitle">Confirma explícitamente antes de restaurar.</p>
              </div>
              <button type="button" (click)="closeRestoreModal()" title="Cerrar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <form class="mt-6 grid gap-4" [formGroup]="restoreForm" (ngSubmit)="submitRestore()">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 text-sm text-[#1B5E20]">
                Estás por restaurar: <strong>{{ backup.fileName || backup.id }}</strong>
              </div>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Confirmación</span>
                <input
                  formControlName="confirmationText"
                  type="text"
                  class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]"
                  placeholder="RESTORE_TENANT_BACKUP">
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Motivo</span>
                <textarea formControlName="reason" rows="3" class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]"></textarea>
              </label>

              <div class="app-modal-footer">
                <button type="button" (click)="closeRestoreModal()" class="cursor-pointer rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  Cancelar
                </button>
                <button type="submit" [disabled]="saving()" class="cursor-pointer rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-60">
                  {{ saving() ? 'Restaurando...' : 'Confirmar restauración' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }

      @if (restoreFromFileModalOpen()) {
        <div class="app-modal-overlay" (click)="closeRestoreFromFileModal()">
          <div class="app-modal-panel app-modal-panel-sm" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Restaurar desde archivo</h2>
                <p class="app-modal-subtitle">Sube un backup descargado y confirma la restauración.</p>
              </div>
              <button type="button" (click)="closeRestoreFromFileModal()" title="Cerrar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <form class="mt-6 grid gap-4" [formGroup]="restoreFromFileForm" (ngSubmit)="submitRestoreFromFile()">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 text-sm text-[#1B5E20]">
                Selecciona el archivo descargado y confirma antes de iniciar.
              </div>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Archivo</span>
                <input
                  type="file"
                  accept=".dump,.backup,.sql,application/octet-stream"
                  (change)="onRestoreFromFileSelected($event)"
                  class="block w-full cursor-pointer rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] file:mr-4 file:rounded-full file:border-0 file:bg-[#F1F8E9] file:px-4 file:py-2 file:text-sm file:font-semibold file:text-[#2E7D32] hover:file:bg-[#E8F5E9]" />
                <p class="mt-2 text-xs text-[#6B7D6C]">
                  {{ restoreFromFileFileName() || 'Ningún archivo seleccionado' }}
                </p>
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Confirmación</span>
                <input
                  formControlName="confirmationText"
                  type="text"
                  class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]"
                  placeholder="RESTORE_TENANT_BACKUP">
              </label>

              <label>
                <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Motivo</span>
                <textarea formControlName="reason" rows="3" class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]"></textarea>
              </label>

              <div class="app-modal-footer">
                <button type="button" (click)="closeRestoreFromFileModal()" class="cursor-pointer rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                  Cancelar
                </button>
                <button type="submit" [disabled]="saving()" class="cursor-pointer rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-60">
                  {{ saving() ? 'Restaurando...' : 'Confirmar restauración' }}
                </button>
              </div>
            </form>
          </div>
        </div>
      }
    </section>
  `
})
export class BackupsPageComponent implements OnInit, OnDestroy {
  private readonly platformService = inject(PlatformService);
  private readonly fb = inject(FormBuilder);
  private readonly toast = inject(ToastService);
  private readonly pageSize = 20;

  backups = signal<PlatformBackup[]>([]);
  backupPage = signal<PageResponse<PlatformBackup> | null>(null);
  createModalOpen = signal(false);
  detailModalOpen = signal(false);
  restoreModalOpen = signal(false);
  restoreFromFileModalOpen = signal(false);
  selectedBackup = signal<PlatformBackup | null>(null);
  restoreFromFileFile = signal<File | null>(null);
  loading = signal(false);
  saving = signal(false);
  error = signal<string | null>(null);
  private autoRefreshTimer: number | null = null;

  readonly stats = computed(() => {
    const items = this.backups();
    return {
      total: items.length,
      completed: items.filter((item) => item.status === 'COMPLETED' || item.status === 'RESTORED' || item.status === 'RESTORED_WITH_WARNINGS').length,
      running: items.filter((item) => item.status === 'RUNNING' || item.status === 'PENDING' || item.status === 'RESTORING').length,
      failed: items.filter((item) => item.status === 'FAILED' || item.status === 'RESTORE_FAILED').length
    };
  });

  readonly createForm = this.fb.nonNullable.group({
    reason: ['']
  });

  readonly restoreForm = this.fb.nonNullable.group({
    confirmationText: ['RESTORE_TENANT_BACKUP', [Validators.required]],
    reason: ['']
  });

  readonly restoreFromFileForm = this.fb.nonNullable.group({
    confirmationText: ['RESTORE_TENANT_BACKUP', [Validators.required]],
    reason: ['']
  });

  readonly restoreFromFileFileName = computed(() => this.restoreFromFileFile()?.name ?? '');

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.backupPage()?.number ?? 0): Promise<void> {
    this.loading.set(true);
    this.error.set(null);

    try {
      const response = await firstValueFrom(
        this.platformService.getTenantBackups(page, this.pageSize).pipe(timeout(15000))
      );

      if (response.success && response.data) {
        this.backups.set(response.data.content);
        this.backupPage.set(response.data);
      } else {
        this.error.set(response.message || 'No se pudieron cargar los respaldos');
      }
    } catch (err: any) {
      this.error.set(err?.error?.message || err?.message || 'No se pudieron cargar los respaldos');
    } finally {
      this.loading.set(false);
      if (this.hasRunningJobs()) {
        this.scheduleAutoRefresh();
      } else if (this.autoRefreshTimer !== null) {
        window.clearTimeout(this.autoRefreshTimer);
        this.autoRefreshTimer = null;
      }
    }
  }

  private scheduleAutoRefresh(delayMs = 4000): void {
    if (this.autoRefreshTimer !== null) {
      window.clearTimeout(this.autoRefreshTimer);
    }

    this.autoRefreshTimer = window.setTimeout(async () => {
      this.autoRefreshTimer = null;
      await this.reload();

      if (this.hasRunningJobs()) {
        this.scheduleAutoRefresh();
      }
    }, delayMs);
  }

  private hasRunningJobs(): boolean {
    return this.backups().some((backup) =>
      backup.status === 'PENDING' || backup.status === 'RUNNING' || backup.status === 'RESTORING'
    );
  }

  async changePage(page: number): Promise<void> {
    await this.reload(page);
  }

  openCreateModal(): void {
    this.createForm.reset({ reason: '' });
    this.createModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.createModalOpen.set(false);
  }

  async submitCreate(): Promise<void> {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    try {
      const { reason } = this.createForm.getRawValue();
      const response = await firstValueFrom(
        this.platformService.createTenantSelfBackup(reason || null)
      );

      if (response.success) {
        this.toast.success('Respaldo solicitado correctamente');
        await this.reload();
        this.closeCreateModal();
      } else {
        this.toast.error(response.message || 'No se pudo crear el respaldo');
      }
    } catch (err: any) {
      this.toast.error(err?.error?.message || err?.message || 'No se pudo crear el respaldo');
    } finally {
      this.saving.set(false);
    }
  }

  openDetail(backup: PlatformBackup): void {
    this.selectedBackup.set(backup);
    this.detailModalOpen.set(true);
  }

  closeDetail(): void {
    this.detailModalOpen.set(false);
  }

  openRestoreModal(backup: PlatformBackup): void {
    this.selectedBackup.set(backup);
    this.restoreForm.reset({
      confirmationText: 'RESTORE_TENANT_BACKUP',
      reason: ''
    });
    this.restoreModalOpen.set(true);
  }

  closeRestoreModal(): void {
    this.restoreModalOpen.set(false);
  }

  openRestoreFromFileModal(): void {
    this.restoreFromFileForm.reset({
      confirmationText: 'RESTORE_TENANT_BACKUP',
      reason: ''
    });
    this.restoreFromFileFile.set(null);
    this.restoreFromFileModalOpen.set(true);
  }

  closeRestoreFromFileModal(): void {
    this.restoreFromFileModalOpen.set(false);
  }

  onRestoreFromFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0] ?? null;
    this.restoreFromFileFile.set(file);
  }

  async submitRestore(): Promise<void> {
    if (!this.selectedBackup()) {
      return;
    }

    if (this.restoreForm.invalid) {
      this.restoreForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    try {
      const { confirmationText, reason } = this.restoreForm.getRawValue();
      const response = await firstValueFrom(
        this.platformService.restoreTenantBackup(this.selectedBackup()!.id, {
          confirmationText,
          reason: reason || null
        })
      );

      if (response.success) {
        this.toast.info('Restauración solicitada correctamente. Revisa el estado del respaldo en la tabla.');
        await this.reload();
        this.closeRestoreModal();
      } else {
        this.toast.error(response.message || 'No se pudo restaurar el respaldo');
      }
    } catch (err: any) {
      this.toast.error(err?.error?.message || err?.message || 'No se pudo restaurar el respaldo');
    } finally {
      this.saving.set(false);
    }
  }

  async submitRestoreFromFile(): Promise<void> {
    const file = this.restoreFromFileFile();
    if (!file) {
      this.toast.error('Selecciona un archivo de respaldo');
      return;
    }

    if (this.restoreFromFileForm.invalid) {
      this.restoreFromFileForm.markAllAsTouched();
      return;
    }

    this.saving.set(true);
    try {
      const { confirmationText, reason } = this.restoreFromFileForm.getRawValue();
      const response = await firstValueFrom(
        this.platformService.restoreTenantBackupFromFile(file, {
          confirmationText,
          reason: reason || null
        })
      );

      if (response.success) {
        this.toast.info('Restauración desde archivo solicitada correctamente. Revisa el estado en la tabla.');
        await this.reload();
        this.closeRestoreFromFileModal();
      } else {
        this.toast.error(response.message || 'No se pudo restaurar desde el archivo');
      }
    } catch (err: any) {
      this.toast.error(err?.error?.message || err?.message || 'No se pudo restaurar desde el archivo');
    } finally {
      this.saving.set(false);
    }
  }

  ngOnDestroy(): void {
    if (this.autoRefreshTimer !== null) {
      window.clearTimeout(this.autoRefreshTimer);
      this.autoRefreshTimer = null;
    }
  }

  async download(backup: PlatformBackup): Promise<void> {
    try {
      const blob = await firstValueFrom(this.platformService.downloadTenantBackup(backup.id));
      const url = window.URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href = url;
      link.download = backup.fileName || `backup-${backup.id}.sql`;
      link.click();
      window.URL.revokeObjectURL(url);
    } catch (err: any) {
      this.toast.error(err?.error?.message || err?.message || 'No se pudo descargar el respaldo');
    }
  }

  statusLabel(status: string): string {
    const labels: Record<string, string> = {
      COMPLETED: 'Completado',
      RESTORED: 'Restaurado',
      RESTORED_WITH_WARNINGS: 'Restaurado con avisos',
      RUNNING: 'En proceso',
      PENDING: 'Pendiente',
      RESTORING: 'Restaurando',
      FAILED: 'Fallido',
      RESTORE_FAILED: 'Restauración fallida'
    };

    return labels[status] ?? status;
  }

  statusClass(status: string): string {
    if (status === 'COMPLETED' || status === 'RESTORED') {
      return 'border-[#C8E6C9] bg-[#F1F8E9] text-[#2E7D32]';
    }

    if (status === 'RESTORED_WITH_WARNINGS') {
      return 'border-[#FFE0A3] bg-[#FFF8E6] text-[#9A6700]';
    }

    if (status === 'RUNNING' || status === 'PENDING' || status === 'RESTORING') {
      return 'border-[#FFD59E] bg-[#FFF7E6] text-[#B26A00]';
    }

    return 'border-[#F3C6C6] bg-[#FFF7F7] text-[#B42318]';
  }

  operationLabel(operationType: string): string {
    const labels: Record<string, string> = {
      FULL_BACKUP: 'Backup full',
      TENANT_BACKUP: 'Backup tenant',
      RESTORE: 'Restauración'
    };

    return labels[operationType] ?? operationType;
  }

  formatBytes(bytes: number | null | undefined): string {
    if (!bytes || bytes <= 0) {
      return 'N/A';
    }

    const units = ['B', 'KB', 'MB', 'GB', 'TB'];
    let value = bytes;
    let index = 0;

    while (value >= 1024 && index < units.length - 1) {
      value /= 1024;
      index += 1;
    }

    return `${value.toFixed(index === 0 ? 0 : 1)} ${units[index]}`;
  }

  formatDate(value: string | null | undefined): string {
    if (!value) {
      return 'N/A';
    }

    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
      return 'N/A';
    }

    return new Intl.DateTimeFormat('es-BO', {
      timeZone: 'America/La_Paz',
      dateStyle: 'medium',
      timeStyle: 'short'
    }).format(date);
  }
}

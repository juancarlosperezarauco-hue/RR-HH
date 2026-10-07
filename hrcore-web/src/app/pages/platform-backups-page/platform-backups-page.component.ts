import { CommonModule } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnDestroy, OnInit, computed, inject, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { firstValueFrom, timeout } from 'rxjs';
import { PageResponse, PlatformBackup, PlatformService, PlatformTenant } from '../../entities/platform/api/platform.service';
import { PlatformPaginationComponent } from '../../features/platform/ui/platform-pagination/platform-pagination.component';
import { ToastService } from '../../shared/ui/toast/toast.service';

type BackupScope = 'FULL_DATABASE' | 'TENANT_SCHEMA';

@Component({
  selector: 'app-platform-backups-page',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, LucideAngularModule, PlatformPaginationComponent],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Plataforma superadmin
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Respaldos
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Gestiona backups globales y por tenant, descarga artefactos y revisa estado.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="openCreateModal('FULL_DATABASE')"
              title="Nuevo"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full bg-[#1B5E20] px-4 py-2 text-sm font-semibold text-white transition-colors hover:bg-[#2E7D32] disabled:cursor-not-allowed disabled:opacity-60">
              <lucide-icon name="arrow-down-to-line" class="h-4 w-4"></lucide-icon>
              Nuevo respaldo full
            </button>
            <button
              type="button"
              (click)="openRestoreFromFileModal()"
              title="Restaurar"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="upload" class="h-4 w-4"></lucide-icon>
              Restaurar desde archivo
            </button>
            <button
              type="button"
              (click)="reload()"
              title="Actualizar"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" class="h-4 w-4"></lucide-icon>
              Recargar
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Respaldos en página</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Cargados en la vista actual</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Completados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().completed }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Listos para descargar o restaurar</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">En proceso</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().running }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Jobs activos o pendientes</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Fallidos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().failed }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Revisar razón de fallo</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="mb-4 flex flex-col gap-3 lg:flex-row lg:items-center lg:justify-between">
          <div>
            <h2 class="text-lg font-bold text-[#1B5E20]">Listado de respaldos</h2>
            <p class="text-sm text-[#6B7D6C]">Descarga, detalle y restauración desde una sola vista.</p>
          </div>
          <div class="flex flex-wrap gap-2">
            <button
              type="button"
              (click)="openCreateModal('TENANT_SCHEMA')"
              title="Tenant"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-[#F1F8E9] px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#E8F5E9]">
              <lucide-icon name="building-2" class="h-4 w-4"></lucide-icon>
              Respaldo por tenant
            </button>
          </div>
        </div>

        <div class="overflow-x-auto">
          <table class="min-w-[1100px] w-full divide-y divide-[#E8F2E2]">
            <thead class="bg-[#FAFCF8]">
              <tr>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Fecha</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Scope</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Tenant</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Estado</th>
                <th class="px-4 py-3 text-left text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Archivo</th>
                <th class="px-4 py-3 text-right text-xs font-semibold uppercase tracking-[0.12em] text-[#567157]">Acciones</th>
              </tr>
            </thead>
            <tbody class="divide-y divide-[#F1F5EF]">
              @for (backup of filteredBackups(); track backup.id) {
                <tr class="hover:bg-[#FAFCF8]">
                  <td class="px-4 py-4 text-sm text-[#1B5E20]">{{ formatDate(backup.createdAt) }}</td>
                  <td class="px-4 py-4 text-sm font-semibold text-[#1B5E20]">{{ backup.scope }}</td>
                  <td class="px-4 py-4 text-sm text-[#1B5E20]">
                    <div class="font-semibold">{{ backup.tenantSlug || 'GLOBAL' }}</div>
                    <div class="text-xs text-[#6B7D6C]">{{ backup.schemaName || 'public' }}</div>
                  </td>
                  <td class="px-4 py-4 text-sm">
                    <span class="rounded-full border px-3 py-1 text-xs font-semibold"
                      [class.border-[#C8E6C9]]="isSuccessStatus(backup.status)"
                      [class.bg-[#F1F8E9]]="isSuccessStatus(backup.status)"
                      [class.text-[#2E7D32]]="isSuccessStatus(backup.status)"
                      [class.border-[#FFE0A3]]="isWarningStatus(backup.status)"
                      [class.bg-[#FFF8E6]]="isWarningStatus(backup.status)"
                      [class.text-[#9A6700]]="isWarningStatus(backup.status)"
                      [class.border-[#FFD59E]]="isRunningStatus(backup.status)"
                      [class.bg-[#FFF7E6]]="isRunningStatus(backup.status)"
                      [class.text-[#B26A00]]="isRunningStatus(backup.status)"
                      [class.border-[#F3C6C6]]="backup.status === 'FAILED'"
                      [class.bg-[#FFF7F7]]="backup.status === 'FAILED'"
                      [class.text-[#B42318]]="backup.status === 'FAILED'">
                      {{ statusLabel(backup.status) }}
                    </span>
                  </td>
                  <td class="px-4 py-4 text-sm text-[#1B5E20]">
                    <div class="font-semibold">{{ backup.fileName || 'N/A' }}</div>
                    <div class="text-xs text-[#6B7D6C]">{{ backup.format || 'N/A' }}</div>
                  </td>
                  <td class="px-4 py-4 text-right text-sm font-medium">
                    <div class="inline-flex items-center gap-2">
                      <button type="button" (click)="openDetail(backup)" title="Detalle" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                        <lucide-icon name="eye" class="h-4 w-4"></lucide-icon>
                      </button>
                      <button type="button" (click)="download(backup)" title="Descargar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                        <lucide-icon name="arrow-down-to-line" class="h-4 w-4"></lucide-icon>
                      </button>
                      <button type="button" (click)="openRestoreModal(backup)" title="Restaurar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                        <lucide-icon name="rotate-ccw" class="h-4 w-4"></lucide-icon>
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        </div>

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

        @if (filteredBackups().length === 0) {
          <div class="rounded-2xl border border-dashed border-[#C8E6C9] bg-[#FAFCF8] px-4 py-8 text-sm text-[#6B7D6C]">
            No hay respaldos que coincidan con los filtros actuales.
          </div>
        }
      </section>

      @if (createModalOpen()) {
        <div class="app-modal-overlay" (click)="closeCreateModal()">
          <div class="app-modal-panel app-modal-panel-sm" (click)="$event.stopPropagation()">
            <div class="app-modal-header">
              <div>
                <h2 class="app-modal-title">Nuevo respaldo</h2>
                <p class="app-modal-subtitle">Crea un respaldo full o por tenant.</p>
              </div>
              <button type="button" (click)="closeCreateModal()" title="Cerrar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <form class="mt-6 grid gap-4" [formGroup]="createForm" (ngSubmit)="submitCreate()">
              <div class="grid gap-2 sm:grid-cols-2">
                <button type="button" (click)="setScope('FULL_DATABASE')" class="cursor-pointer rounded-2xl border px-4 py-3 text-left transition-colors"
                  [class.border-[#2E7D32]]="createForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.bg-[#F1F8E9]]="createForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.text-[#1B5E20]]="createForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.border-[#DDEED8]]="createForm.controls.scope.value !== 'FULL_DATABASE'">
                  <p class="text-sm font-semibold">Full database</p>
                  <p class="text-xs text-[#6B7D6C]">Respaldo completo de plataforma</p>
                </button>
                <button type="button" (click)="setScope('TENANT_SCHEMA')" class="cursor-pointer rounded-2xl border px-4 py-3 text-left transition-colors"
                  [class.border-[#2E7D32]]="createForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.bg-[#F1F8E9]]="createForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.text-[#1B5E20]]="createForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.border-[#DDEED8]]="createForm.controls.scope.value !== 'TENANT_SCHEMA'">
                  <p class="text-sm font-semibold">Tenant schema</p>
                  <p class="text-xs text-[#6B7D6C]">Respaldo puntual por tenant</p>
                </button>
              </div>

              @if (createForm.controls.scope.value === 'TENANT_SCHEMA') {
                <label>
                  <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Tenant</span>
                  <select formControlName="tenantId" class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]">
                    <option value="">Selecciona un tenant</option>
                    @for (tenant of tenants(); track tenant.id) {
                      <option [value]="tenant.id">{{ tenant.name }} ({{ tenant.slug }})</option>
                    }
                  </select>
                </label>
              }

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
                <p class="app-modal-subtitle">Información operativa y trazabilidad del job.</p>
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
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Tenant</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.tenantSlug || 'GLOBAL' }}</p>
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
            </div>

            <div class="mt-4 grid gap-4 sm:grid-cols-2">
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Requested by</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.requestedBy || 'N/A' }}</p>
              </div>
              <div class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4">
                <p class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Checksum</p>
                <p class="mt-1 break-words text-sm font-semibold text-[#1B5E20]">{{ backup.checksumSha256 || 'N/A' }}</p>
              </div>
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
                <input formControlName="confirmationText" type="text" class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]" placeholder="RESTORE_FULL_DATABASE o RESTORE_PLATFORM_BACKUP">
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
                <p class="app-modal-subtitle">Sube un backup descargado y define el alcance del restore.</p>
              </div>
              <button type="button" (click)="closeRestoreFromFileModal()" title="Cerrar" class="cursor-pointer rounded-full border border-[#DDEED8] p-2 text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
                <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
              </button>
            </div>

            <form class="mt-6 grid gap-4" [formGroup]="restoreFromFileForm" (ngSubmit)="submitRestoreFromFile()">
              <div class="grid gap-2 sm:grid-cols-2">
                <button
                  type="button"
                  (click)="setRestoreFromFileScope('FULL_DATABASE')"
                  class="cursor-pointer rounded-2xl border px-4 py-3 text-left transition-colors"
                  [class.border-[#2E7D32]]="restoreFromFileForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.bg-[#F1F8E9]]="restoreFromFileForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.text-[#1B5E20]]="restoreFromFileForm.controls.scope.value === 'FULL_DATABASE'"
                  [class.border-[#DDEED8]]="restoreFromFileForm.controls.scope.value !== 'FULL_DATABASE'">
                  <p class="text-sm font-semibold">Full database</p>
                  <p class="text-xs text-[#6B7D6C]">Restaura toda la base</p>
                </button>
                <button
                  type="button"
                  (click)="setRestoreFromFileScope('TENANT_SCHEMA')"
                  class="cursor-pointer rounded-2xl border px-4 py-3 text-left transition-colors"
                  [class.border-[#2E7D32]]="restoreFromFileForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.bg-[#F1F8E9]]="restoreFromFileForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.text-[#1B5E20]]="restoreFromFileForm.controls.scope.value === 'TENANT_SCHEMA'"
                  [class.border-[#DDEED8]]="restoreFromFileForm.controls.scope.value !== 'TENANT_SCHEMA'">
                  <p class="text-sm font-semibold">Tenant schema</p>
                  <p class="text-xs text-[#6B7D6C]">Restaura un tenant específico</p>
                </button>
              </div>

              @if (restoreFromFileForm.controls.scope.value === 'TENANT_SCHEMA') {
                <label>
                  <span class="mb-1 block text-sm font-semibold text-[#2E7D32]">Tenant</span>
                  <select
                    formControlName="tenantId"
                    class="w-full rounded-2xl border border-[#C8E6C9] bg-white px-4 py-3 text-sm text-[#1B5E20] outline-none focus:border-[#2E7D32]">
                    <option value="">Selecciona un tenant</option>
                    @for (tenant of tenants(); track tenant.id) {
                      <option [value]="tenant.id">{{ tenant.name }} ({{ tenant.slug }})</option>
                    }
                  </select>
                </label>
              }

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
                  [placeholder]="restoreFromFileForm.controls.scope.value === 'FULL_DATABASE' ? 'RESTORE_FULL_DATABASE' : 'RESTORE_PLATFORM_BACKUP'">
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
    </div>
  `
})
export class PlatformBackupsPageComponent implements OnInit, OnDestroy {
  private readonly platformService = inject(PlatformService);
  private readonly toast = inject(ToastService);
  private readonly fb = inject(FormBuilder);
  private readonly backupPageSize = 20;
  private readonly tenantSelectSize = 1000;
  private autoRefreshTimer: number | null = null;

  backups = signal<PlatformBackup[]>([]);
  backupPage = signal<PageResponse<PlatformBackup> | null>(null);
  tenants = signal<PlatformTenant[]>([]);
  createModalOpen = signal(false);
  detailModalOpen = signal(false);
  restoreModalOpen = signal(false);
  restoreFromFileModalOpen = signal(false);
  selectedBackup = signal<PlatformBackup | null>(null);
  restoreFromFileFile = signal<File | null>(null);
  saving = signal(false);
  loading = signal(false);

  readonly filteredBackups = computed(() => this.backups().slice().sort((left, right) => right.createdAt.localeCompare(left.createdAt)));
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
    scope: ['FULL_DATABASE' as BackupScope, Validators.required],
    tenantId: [''],
    reason: ['']
  });

  readonly restoreForm = this.fb.nonNullable.group({
    confirmationText: ['RESTORE_FULL_DATABASE', [Validators.required]],
    reason: ['']
  });

  readonly restoreFromFileForm = this.fb.nonNullable.group({
    scope: ['FULL_DATABASE' as BackupScope, Validators.required],
    tenantId: [''],
    confirmationText: ['RESTORE_FULL_DATABASE', [Validators.required]],
    reason: ['']
  });

  readonly restoreFromFileFileName = computed(() => this.restoreFromFileFile()?.name ?? '');

  ngOnInit(): void {
    void this.reload();
  }

  async reload(page = this.backupPage()?.number ?? 0): Promise<void> {
    this.loading.set(true);
    try {
      const [backupsResponse, tenantsResponse] = await Promise.all([
        firstValueFrom(this.platformService.getBackups(page, this.backupPageSize).pipe(timeout(15000))),
        firstValueFrom(this.platformService.getTenants(0, this.tenantSelectSize).pipe(timeout(15000)))
      ]);

      if (backupsResponse.success && backupsResponse.data) {
        this.backups.set(backupsResponse.data.content);
        this.backupPage.set(backupsResponse.data);
      }

      if (tenantsResponse.success && tenantsResponse.data) {
        this.tenants.set(tenantsResponse.data.content);
      }
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

  openCreateModal(scope: BackupScope): void {
    this.createForm.reset({ scope, tenantId: '', reason: '' });
    this.createModalOpen.set(true);
  }

  closeCreateModal(): void {
    this.createModalOpen.set(false);
  }

  setScope(scope: BackupScope): void {
    this.createForm.patchValue({ scope });
  }

  async submitCreate(): Promise<void> {
    if (this.createForm.invalid) {
      this.createForm.markAllAsTouched();
      return;
    }

    const { scope, tenantId, reason } = this.createForm.getRawValue();
    this.saving.set(true);
    try {
      const response = scope === 'FULL_DATABASE'
        ? await firstValueFrom(this.platformService.createFullBackup(reason || null))
        : await firstValueFrom(this.platformService.createTenantBackup(tenantId || '', reason || null));

      if (response.success) {
        await this.reload();
        this.closeCreateModal();
        this.toast.success('Respaldo solicitado correctamente');
        if (this.hasRunningJobs()) {
          this.scheduleAutoRefresh();
        }
      }
    } catch (err) {
      if (err instanceof HttpErrorResponse && err.status === 409) {
        this.toast.warning('Ya existe un respaldo en proceso');
        await this.reload();
        return;
      }

      const message = err instanceof HttpErrorResponse
        ? err.error?.message || err.message
        : err instanceof Error
          ? err.message
          : null;
      this.toast.error(message || 'No se pudo crear el respaldo');
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
    this.restoreForm.reset({ confirmationText: backup.scope === 'FULL_DATABASE' ? 'RESTORE_FULL_DATABASE' : 'RESTORE_PLATFORM_BACKUP', reason: '' });
    this.restoreModalOpen.set(true);
  }

  closeRestoreModal(): void {
    this.restoreModalOpen.set(false);
  }

  openRestoreFromFileModal(): void {
    this.restoreFromFileForm.reset({
      scope: 'FULL_DATABASE',
      tenantId: '',
      confirmationText: 'RESTORE_FULL_DATABASE',
      reason: ''
    });
    this.restoreFromFileFile.set(null);
    this.restoreFromFileModalOpen.set(true);
  }

  closeRestoreFromFileModal(): void {
    this.restoreFromFileModalOpen.set(false);
  }

  setRestoreFromFileScope(scope: BackupScope): void {
    this.restoreFromFileForm.patchValue({
      scope,
      confirmationText: scope === 'FULL_DATABASE' ? 'RESTORE_FULL_DATABASE' : 'RESTORE_PLATFORM_BACKUP',
      tenantId: scope === 'FULL_DATABASE' ? '' : this.restoreFromFileForm.controls.tenantId.value
    });
  }

  onRestoreFromFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.restoreFromFileFile.set(input.files?.[0] ?? null);
  }

  async submitRestore(): Promise<void> {
    if (!this.selectedBackup()) {
      return;
    }

    if (this.restoreForm.invalid) {
      this.restoreForm.markAllAsTouched();
      return;
    }

    const { confirmationText, reason } = this.restoreForm.getRawValue();
    this.saving.set(true);
    try {
      const response = await firstValueFrom(
        this.platformService.restoreBackup(this.selectedBackup()!.id, {
          confirmationText,
          reason: reason || null
        })
      );

      if (response.success) {
        await this.reload();
        this.closeRestoreModal();
        if (this.hasRunningJobs()) {
          this.scheduleAutoRefresh();
        }
      }
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

    const { scope, tenantId, confirmationText, reason } = this.restoreFromFileForm.getRawValue();
    if (scope === 'TENANT_SCHEMA' && !tenantId) {
      this.toast.error('Selecciona un tenant para restaurar el archivo');
      return;
    }

    this.saving.set(true);
    try {
      const response = await firstValueFrom(
        this.platformService.restoreBackupFromFile(file, {
          scope,
          tenantId: scope === 'TENANT_SCHEMA' ? tenantId || null : null,
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

  async download(backup: PlatformBackup): Promise<void> {
    const blob = await firstValueFrom(this.platformService.downloadBackup(backup.id));
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = backup.fileName || `backup-${backup.id}.sql`;
    link.click();
    window.URL.revokeObjectURL(url);
  }

  ngOnDestroy(): void {
    if (this.autoRefreshTimer !== null) {
      window.clearTimeout(this.autoRefreshTimer);
      this.autoRefreshTimer = null;
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

  isSuccessStatus(status: string): boolean {
    return status === 'COMPLETED' || status === 'RESTORED';
  }

  isWarningStatus(status: string): boolean {
    return status === 'RESTORED_WITH_WARNINGS';
  }

  isRunningStatus(status: string): boolean {
    return status === 'RUNNING' || status === 'PENDING' || status === 'RESTORING';
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

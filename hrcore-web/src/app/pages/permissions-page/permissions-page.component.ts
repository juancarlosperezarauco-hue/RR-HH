import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PermissionListUseCase } from '../../features/role-management';

interface PermissionGroupView {
  module: string;
  permissions: { code: string; module: string; description: string }[];
}

@Component({
  selector: 'app-permissions-page',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <div class="space-y-6 p-4 sm:p-6 lg:p-8 relative">
      <section class="rounded-[28px] border border-[#C8E6C9] bg-gradient-to-br from-white via-[#F7FBF3] to-[#EAF6EB] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
          <div class="space-y-3">
            <div class="inline-flex items-center gap-2 rounded-full border border-[#C8E6C9] bg-white/80 px-3 py-1 text-xs font-semibold uppercase tracking-[0.16em] text-[#2E7D32]">
              <span class="h-2 w-2 rounded-full bg-[#4CAF50]"></span>
              Catálogo de seguridad
            </div>
            <div>
              <h1 class="text-3xl font-black tracking-tight text-[#1B5E20] sm:text-4xl">
                Permisos del sistema
              </h1>
              <p class="mt-2 max-w-3xl text-sm leading-6 text-[#5F6F5F]">
                Catálogo base de permisos que alimenta roles, guards y asignaciones de acceso del tenant.
              </p>
            </div>
          </div>

          <div class="flex flex-wrap gap-3">
            <button
              type="button"
              (click)="reload()"
              class="inline-flex cursor-pointer items-center gap-2 rounded-full border border-[#C8E6C9] bg-white px-4 py-2 text-sm font-semibold text-[#2E7D32] transition-colors hover:bg-[#F1F8E9]">
              <lucide-icon name="refresh-ccw" [size]="16"></lucide-icon>
              Recargar
            </button>
          </div>
        </div>
      </section>

      <section class="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Permisos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().total }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Códigos en el catálogo</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Módulos</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().modules }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Agrupaciones disponibles</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Filtrados</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().filtered }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Coinciden con la búsqueda</p>
        </div>
        <div class="rounded-2xl border border-[#C8E6C9] bg-white p-5 shadow-sm">
          <p class="text-sm font-semibold text-[#567157]">Bloques visibles</p>
          <p class="mt-4 text-3xl font-black text-[#1B5E20]">{{ stats().groups }}</p>
          <p class="mt-2 text-xs text-[#6B7D6C]">Secciones renderizadas</p>
        </div>
      </section>

      <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
        <div class="flex flex-col gap-4 lg:flex-row lg:items-center lg:justify-between">
          <div class="space-y-1">
            <h2 class="text-lg font-bold text-[#1B5E20]">Filtros</h2>
            <p class="text-sm text-[#6B7D6C]">Busca por código, módulo o descripción.</p>
          </div>

          <div class="grid gap-3 sm:grid-cols-2">
            <label class="flex min-w-[260px] items-center gap-3 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 py-3">
              <span class="text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">Buscar</span>
              <input
                type="text"
                [ngModel]="searchQuery()"
                (ngModelChange)="searchQuery.set($event)"
                placeholder="Código, módulo o descripción"
                class="w-full border-0 bg-transparent text-sm text-[#1B5E20] outline-none placeholder:text-[#9AA99A]" />
            </label>

            <select
              [ngModel]="moduleFilter()"
              (ngModelChange)="moduleFilter.set($event)"
              class="h-12 rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] px-4 text-sm text-[#1B5E20] outline-none transition-colors focus:border-[#2E7D32] focus:bg-white">
              <option value="all">Todos los módulos</option>
              @for (module of moduleOptions(); track module) {
                <option [value]="module">{{ formatModuleLabel(module) }}</option>
              }
            </select>
          </div>
        </div>
      </section>

      @if (permissionListUseCase.status() === 'loading' && permissionListUseCase.data().length === 0) {
        <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-12 shadow-sm flex flex-col items-center gap-4 text-[#6B7D6C]">
          <svg class="h-8 w-8 animate-spin text-[#2E7D32]" viewBox="0 0 24 24">
            <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
            <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
          </svg>
          <p>Cargando permisos...</p>
        </section>
      }

      @if (permissionListUseCase.status() === 'error') {
        <section class="rounded-2xl border border-red-200 bg-red-50 p-6 flex items-start gap-4">
          <svg xmlns="http://www.w3.org/2000/svg" width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" class="mt-0.5 text-red-700">
            <circle cx="12" cy="12" r="10"></circle>
            <line x1="12" x2="12" y1="8" y2="12"></line>
            <line x1="12" x2="12.01" y1="16" y2="16"></line>
          </svg>
          <div>
            <h3 class="font-semibold text-red-700">Error al cargar permisos</h3>
            <p class="mt-1 text-sm text-red-700/80">{{ permissionListUseCase.error() }}</p>
            <button (click)="reload()" class="mt-3 cursor-pointer text-sm font-semibold text-red-700 hover:underline">Intentar nuevamente</button>
          </div>
        </section>
      }

      @if (permissionListUseCase.status() === 'success' || (permissionListUseCase.status() === 'loading' && permissionListUseCase.data().length > 0)) {
        @if (groupedPermissions().length === 0) {
          <section class="rounded-[24px] border border-dashed border-[#C8E6C9] bg-white px-4 py-10 text-center text-sm text-[#6B7D6C] shadow-sm">
            No hay permisos que coincidan con los filtros actuales.
          </section>
        } @else {
          <div class="space-y-4">
            @for (group of groupedPermissions(); track group.module) {
              <section class="rounded-[24px] border border-[#C8E6C9] bg-white p-4 sm:p-6 shadow-sm">
                <div class="flex flex-col gap-2 border-b border-[#E8F2E2] pb-4 sm:flex-row sm:items-end sm:justify-between">
                  <div>
                    <h2 class="text-lg font-bold text-[#1B5E20]">{{ formatModuleLabel(group.module) }}</h2>
                    <p class="text-sm text-[#6B7D6C]">{{ group.permissions.length }} permisos disponibles</p>
                  </div>
                  <div class="rounded-full border border-[#DDEED8] bg-[#FAFCF8] px-3 py-1 text-xs font-bold uppercase tracking-[0.12em] text-[#6B7D6C]">
                    módulo
                  </div>
                </div>

                <div class="mt-4 grid gap-3 md:grid-cols-2 xl:grid-cols-3">
                  @for (permission of group.permissions; track permission.code) {
                    <article class="rounded-2xl border border-[#DDEED8] bg-[#FAFCF8] p-4 shadow-sm transition-colors hover:bg-[#F1F8E9]">
                      <div class="flex items-start justify-between gap-3">
                        <div class="min-w-0">
                          <p class="break-words text-sm font-semibold text-[#1B5E20]">{{ permission.code }}</p>
                          <p class="mt-1 text-xs uppercase tracking-[0.12em] text-[#6B7D6C]">{{ formatModuleLabel(permission.module) }}</p>
                        </div>
                      </div>
                      <p class="mt-3 text-sm leading-6 text-[#4F5D4F]">{{ permission.description }}</p>
                    </article>
                  }
                </div>
              </section>
            }
          </div>
        }
      }
    </div>
  `
})
export class PermissionsPageComponent implements OnInit {
  readonly permissionListUseCase = inject(PermissionListUseCase);

  readonly searchQuery = signal('');
  readonly moduleFilter = signal<string>('all');

  readonly moduleOptions = computed(() => {
    return [...new Set(this.permissionListUseCase.data().map(permission => permission.module))].sort((a, b) => a.localeCompare(b));
  });

  readonly filteredPermissions = computed(() => {
    const query = this.searchQuery().trim().toLowerCase();
    const moduleFilter = this.moduleFilter();

    return this.permissionListUseCase.data().filter(permission => {
      const queryMatches =
        query.length === 0 ||
        [permission.code, permission.module, permission.description]
          .join(' ')
          .toLowerCase()
          .includes(query);

      const moduleMatches = moduleFilter === 'all' || permission.module === moduleFilter;

      return queryMatches && moduleMatches;
    });
  });

  readonly groupedPermissions = computed<PermissionGroupView[]>(() => {
    const grouped = this.filteredPermissions().reduce((acc, permission) => {
      if (!acc[permission.module]) {
        acc[permission.module] = [];
      }

      acc[permission.module].push(permission);
      return acc;
    }, {} as Record<string, PermissionGroupView['permissions']>);

    return Object.keys(grouped)
      .sort((a, b) => a.localeCompare(b))
      .map(module => ({
        module,
        permissions: grouped[module].slice().sort((a, b) => a.code.localeCompare(b.code))
      }));
  });

  readonly stats = computed(() => ({
    total: this.permissionListUseCase.data().length,
    modules: this.moduleOptions().length,
    filtered: this.filteredPermissions().length,
    groups: this.groupedPermissions().length
  }));

  ngOnInit(): void {
    this.reload();
  }

  reload(): void {
    this.permissionListUseCase.loadPermissions();
  }

  formatModuleLabel(module: string): string {
    if (!module) return 'Sin módulo';
    return module
      .replace(/[-_]/g, ' ')
      .split(' ')
      .filter(Boolean)
      .map(part => part.charAt(0).toUpperCase() + part.slice(1))
      .join(' ');
  }
}

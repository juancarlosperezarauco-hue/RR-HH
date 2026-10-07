import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

interface WebPreviewTab {
  key: string;
  label: string;
  title: string;
  description: string;
  accent: string;
  imageUrl: string;
}

@Component({
  selector: 'app-landing-web-platform',
  standalone: true,
  imports: [CommonModule, RouterModule, LucideAngularModule],
  template: `
    <section id="plataforma-web" class="relative isolate overflow-hidden bg-white px-4 py-20 sm:px-6 lg:px-8">
      <div class="absolute inset-0 -z-10 bg-[radial-gradient(circle_at_12%_24%,rgba(46,125,50,0.10),transparent_30%),radial-gradient(circle_at_88%_70%,rgba(210,150,45,0.10),transparent_32%)]"></div>
      @let preview = activePreview();

      <div class="mx-auto max-w-7xl">
        <div class="grid items-end gap-8 lg:grid-cols-[0.9fr_1.1fr]">
          <div>
            <div class="mb-5 inline-flex w-fit items-center gap-2 rounded-full bg-[#E8F5E9] px-5 py-2 text-sm font-black text-[#2E7D32]">
              <lucide-icon name="layout-dashboard" class="h-4 w-4"></lucide-icon>
              Plataforma web
            </div>

            <h2 class="max-w-2xl text-4xl font-black tracking-tight text-[#101827] sm:text-5xl">
              Gestiona tu organización desde la web
            </h2>

            <p class="mt-5 max-w-2xl text-base leading-8 text-[#405447]">
              El panel web centraliza colaboradores, equipos, roles, auditoría y suscripciones en una sola experiencia para administradores.
            </p>
          </div>

          <div class="grid gap-4 sm:grid-cols-2">
            <div class="rounded-[26px] border border-[#DDEED8] bg-[#FAFCF8] p-5">
              <div class="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
                <lucide-icon name="users" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="font-black text-[#101827]">Personas</h3>
              <p class="mt-2 text-sm leading-6 text-[#405447]">Gestión de colaboradores, roles y permisos.</p>
            </div>

            <div class="rounded-[26px] border border-[#DDEED8] bg-[#FAFCF8] p-5">
              <div class="mb-4 flex h-11 w-11 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
                <lucide-icon name="bar-chart-3" class="h-5 w-5"></lucide-icon>
              </div>
              <h3 class="font-black text-[#101827]">Visibilidad</h3>
              <p class="mt-2 text-sm leading-6 text-[#405447]">Equipos, auditoría y control operativo.</p>
            </div>
          </div>
        </div>

        <div class="mt-12 overflow-hidden rounded-[34px] border border-[#DDEED8] bg-white shadow-[0_30px_90px_rgba(27,94,32,0.14)]">
          <div class="flex flex-col gap-4 border-b border-[#DDEED8] bg-[#F7FBF3] p-4 lg:flex-row lg:items-center lg:justify-between">
            <div class="flex items-center gap-3">
              <span class="flex h-11 w-11 items-center justify-center rounded-2xl bg-[#2E7D32] text-white">
                <lucide-icon name="monitor" class="h-5 w-5"></lucide-icon>
              </span>
              <div>
                <p class="text-sm font-black text-[#101827]">{{ preview.title }}</p>
                <p class="text-xs text-[#405447]">{{ preview.description }}</p>
              </div>
            </div>

            <div class="flex gap-2 overflow-x-auto">
              @for (tab of previews; track tab.key) {
                <button
                  type="button"
                  (click)="activeKey.set(tab.key)"
                  class="shrink-0 rounded-full border px-4 py-2 text-xs font-black transition-colors"
                  [class.bg-[#2E7D32]]="activeKey() === tab.key"
                  [class.text-white]="activeKey() === tab.key"
                  [class.bg-white]="activeKey() !== tab.key"
                  [class.text-[#2E7D32]]="activeKey() !== tab.key"
                  [class.border-[#DDEED8]]="activeKey() !== tab.key">
                  {{ tab.label }}
                </button>
              }
            </div>
          </div>

          <div class="grid gap-0 lg:grid-cols-[1.05fr_0.95fr]">
            <div class="bg-[#102016] p-4 sm:p-5">
              <div class="overflow-hidden rounded-[28px] border border-white/10 bg-[#0B1210] p-4 sm:p-6">
                <div class="flex items-center justify-between border-b border-white/10 pb-4">
                  <div>
                    <p class="text-xs font-black uppercase tracking-[0.24em] text-white/45">Vista previa</p>
                    <h3 class="mt-1 text-2xl font-black text-white">{{ preview.title }}</h3>
                  </div>
                  <div class="flex h-12 w-12 items-center justify-center rounded-2xl" [style.background]="preview.accent">
                    <lucide-icon name="layout-dashboard" class="h-5 w-5 text-white"></lucide-icon>
                  </div>
                </div>

                <div class="mt-5 overflow-hidden rounded-[24px] border border-white/10 bg-[#0F1714]">
                  <img
                    [src]="preview.imageUrl"
                    [alt]="preview.title"
                    class="h-auto w-full object-cover"
                  />
                </div>

                <div class="mt-5 rounded-[24px] border border-white/10 bg-white/5 p-4">
                  <p class="text-xs font-black uppercase tracking-[0.18em] text-white/45">Operación</p>
                  <ul class="mt-3 space-y-2 text-sm text-white/75">
                    <li class="flex items-center gap-2">
                      <lucide-icon name="check-circle" class="h-4 w-4 text-[#A5D6A7]"></lucide-icon>
                      Monitoreo en tiempo real
                    </li>
                    <li class="flex items-center gap-2">
                      <lucide-icon name="check-circle" class="h-4 w-4 text-[#A5D6A7]"></lucide-icon>
                      Flujos rápidos y claros
                    </li>
                    <li class="flex items-center gap-2">
                      <lucide-icon name="check-circle" class="h-4 w-4 text-[#A5D6A7]"></lucide-icon>
                      Control administrativo central
                    </li>
                  </ul>
                </div>
              </div>
            </div>

            <div class="flex items-center justify-center bg-white p-6 sm:p-8">
              <div class="w-full max-w-md rounded-[30px] border border-[#DDEED8] bg-[#FAFCF8] p-6 shadow-[0_18px_50px_rgba(27,94,32,0.08)]">
                <p class="text-xs font-black uppercase tracking-[0.2em] text-[#6B7D6C]">Resumen</p>
                <h4 class="mt-3 text-2xl font-black text-[#101827]">{{ preview.label }}</h4>
                <p class="mt-3 text-sm leading-7 text-[#405447]">
                  {{ preview.description }}
                </p>

                <div class="mt-6 grid gap-3">
                  <div class="rounded-[22px] border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-black uppercase tracking-[0.18em] text-[#6B7D6C]">Dashboard</p>
                    <p class="mt-1 text-sm font-bold text-[#101827]">Indicadores, accesos y actividad diaria</p>
                  </div>
                  <div class="rounded-[22px] border border-[#DDEED8] bg-white p-4">
                    <p class="text-xs font-black uppercase tracking-[0.18em] text-[#6B7D6C]">Operación</p>
                    <p class="mt-1 text-sm font-bold text-[#101827]">Auditoría y configuración sin fricción</p>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="mt-10 flex flex-col justify-center gap-3 sm:flex-row">
          <a
            routerLink="/prices"
            class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-[#2E7D32] px-5 text-sm font-black text-white shadow-sm transition-colors hover:bg-[#256428]">
            Ver planes
            <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
          </a>

          <a
            routerLink="/login"
            class="inline-flex h-11 items-center justify-center rounded-xl border border-[#C8E6C9] bg-white px-5 text-sm font-black text-[#1B5E20] shadow-sm transition-colors hover:bg-[#F7FBF3]">
            Ingresar al panel
          </a>
        </div>
      </div>
    </section>
  `
})
export class LandingWebPlatformComponent {
  readonly activeKey = signal('dashboard');

  readonly previews: WebPreviewTab[] = [
    {
      key: 'dashboard',
      label: 'Dashboard',
      title: 'Dashboard administrativo',
      description: 'KPIs, actividad y visión general de la operación.',
      accent: 'linear-gradient(135deg, #2E7D32 0%, #1B5E20 100%)',
      imageUrl: '/landing/web-dashboard.png'
    },
    {
      key: 'accounts',
      label: 'Personas',
      title: 'Gestión de colaboradores',
      description: 'Directorio, perfiles y estados del equipo.',
      accent: 'linear-gradient(135deg, #256428 0%, #4F8C3E 100%)',
      imageUrl: '/landing/web-accounts.png'
    },
    {
      key: 'payments',
      label: 'Equipos',
      title: 'Organización de equipos',
      description: 'Consulta áreas, responsables y estructura del equipo.',
      accent: 'linear-gradient(135deg, #7A5A1E 0%, #D2962D 100%)',
      imageUrl: '/landing/web-payments.png'
    },
  ];

  activePreview(): WebPreviewTab {
    return this.previews.find((item) => item.key === this.activeKey()) ?? this.previews[0];
  }
}

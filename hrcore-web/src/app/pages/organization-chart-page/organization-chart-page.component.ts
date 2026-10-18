import { CommonModule } from '@angular/common';
import { Component, inject, OnInit } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';
import { OrganizationChartNode, OrganizationalTeamService } from '../../entities/organizational-team';

@Component({
  selector: 'app-organization-chart-page',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  styles: [`
    .org-children { position: relative; display: flex; justify-content: center; gap: 1.5rem; padding-top: 2.25rem; }
    .org-children::before { content: ''; position: absolute; top: 1.1rem; left: 50%; width: 1px; height: 1.15rem; background: var(--border); }
    .org-child { position: relative; min-width: 15rem; max-width: 17rem; padding-top: 1rem; }
    .org-child::before { content: ''; position: absolute; top: 0; left: 50%; width: 1px; height: 1rem; background: var(--border); }
    .org-child::after { content: ''; position: absolute; top: 0; left: 0; right: 0; border-top: 1px solid var(--border); }
    .org-child:first-child::after { left: 50%; }
    .org-child:last-child::after { right: 50%; }
    .org-child:only-child::after { display: none; }
    .org-card { width: 15rem; min-height: 4.5rem; border-radius: .65rem; padding: .7rem 1rem; text-align: center; }
    .org-root { background: var(--foreground); border-color: var(--foreground); color: var(--background); }
    .org-manager { background: color-mix(in srgb, var(--primary) 18%, var(--card)); border-color: color-mix(in srgb, var(--primary) 42%, var(--border)); }
    .org-member { background: var(--card); border-color: var(--border); }
    .org-name { color: var(--foreground); font-size: .78rem; font-weight: 700; line-height: 1.2; }
    .org-role { color: var(--muted-foreground); font-size: .68rem; line-height: 1.25; margin-top: .25rem; }
    .org-root .org-name, .org-root .org-role { color: var(--background); }
    .org-manager .org-name { color: var(--foreground); }
    .org-meta { color: var(--muted-foreground); font-size: .62rem; line-height: 1.2; margin-top: .3rem; }
  `],
  template: `
    <section class="space-y-6 p-6">
      <header class="flex flex-col gap-4 rounded-[28px] border border-border bg-gradient-to-br from-card via-background to-muted p-6 shadow-sm lg:flex-row lg:items-end lg:justify-between">
        <div>
          <p class="text-xs font-semibold uppercase tracking-[0.16em] text-primary">Estructura organizacional</p>
          <h1 class="mt-2 text-3xl font-black tracking-tight text-accent-foreground">Organigrama</h1>
          <p class="mt-2 max-w-2xl text-sm leading-6 text-muted-foreground">Visualiza las líneas de supervisión y la distribución jerárquica de tu equipo.</p>
        </div>
        <div class="flex items-center gap-3">
          <div class="rounded-2xl border border-border bg-card px-4 py-3 text-right">
            <p class="text-2xl font-black text-accent-foreground">{{ totalEmployees }}</p>
            <p class="text-[11px] font-semibold uppercase tracking-wider text-muted-foreground">Colaboradores</p>
          </div>
          <button type="button" (click)="reload()" [disabled]="loading" class="inline-flex h-11 items-center gap-2 rounded-full border border-border bg-card px-4 text-sm font-semibold text-primary transition-colors hover:bg-accent disabled:cursor-not-allowed disabled:opacity-50">
            <lucide-icon name="refresh-ccw" [size]="16" [class.animate-spin]="loading"></lucide-icon> Actualizar
          </button>
        </div>
      </header>

      <div class="flex items-center justify-between text-xs text-muted-foreground">
        <span>Relaciones de supervisión activas</span><span *ngIf="asOfDate">Corte: {{ asOfDate }}</span>
      </div>

      <section class="overflow-x-auto rounded-[28px] border border-border bg-card shadow-sm">
        <div class="min-w-[900px] p-8">
          <div *ngIf="loading" class="flex min-h-56 flex-col items-center justify-center gap-3 text-muted-foreground">
            <lucide-icon name="loader-circle" [size]="28" class="animate-spin text-primary"></lucide-icon><p class="text-sm">Cargando estructura organizacional...</p>
          </div>
          <div *ngIf="!loading && error" class="flex min-h-56 flex-col items-center justify-center gap-3 text-center">
            <lucide-icon name="triangle-alert" [size]="28" class="text-destructive"></lucide-icon><p class="text-sm font-semibold text-foreground">No se pudo cargar el organigrama.</p>
            <button type="button" (click)="reload()" class="rounded-full bg-primary px-4 py-2 text-xs font-semibold text-primary-foreground">Reintentar</button>
          </div>
          <div *ngIf="!loading && !error && !nodes.length" class="flex min-h-56 flex-col items-center justify-center gap-2 text-center">
            <lucide-icon name="network" [size]="30" class="text-muted-foreground"></lucide-icon><p class="text-sm font-semibold text-foreground">Aún no hay una estructura para mostrar.</p>
            <p class="max-w-sm text-xs leading-5 text-muted-foreground">Asigna responsables y colaboradores para construir la jerarquía del equipo.</p>
          </div>
          <div *ngIf="!loading && !error && nodes.length" class="space-y-10">
            <ng-container *ngFor="let node of nodes; trackBy: trackNode"><ng-container *ngTemplateOutlet="branch; context: { $implicit: node, level: 0 }"></ng-container></ng-container>
          </div>
        </div>
      </section>

      <ng-template #branch let-node let-level="level">
        <article class="flex flex-col items-center">
          <div class="org-card border shadow-sm transition-shadow hover:shadow-md"
               [class.org-root]="level === 0"
               [class.org-manager]="level === 1"
               [class.org-member]="level > 1">
            <p class="org-name truncate">{{ node.fullName }}</p>
            <p class="org-role truncate">{{ node.positionName || 'Cargo no asignado' }}</p>
            <p *ngIf="level > 1" class="org-meta truncate">{{ node.departmentName || 'Sin departamento' }}</p>
          </div>
          <div *ngIf="node.children?.length" class="org-children">
            <div *ngFor="let child of node.children; trackBy: trackNode" class="org-child"><ng-container *ngTemplateOutlet="branch; context: { $implicit: child, level: level + 1 }"></ng-container></div>
          </div>
        </article>
      </ng-template>
    </section>
  `
})
export class OrganizationChartPageComponent implements OnInit {
  private readonly service = inject(OrganizationalTeamService);
  nodes: OrganizationChartNode[] = [];
  totalEmployees = 0;
  asOfDate = '';
  loading = true;
  error = false;

  ngOnInit(): void { this.loadChart(); }
  reload(): void { this.loadChart(); }
  initials(name: string): string { return name.split(' ').filter(Boolean).slice(0, 2).map(part => part[0]).join('').toUpperCase(); }
  trackNode(_: number, node: OrganizationChartNode): string { return node.employeeId; }

  private loadChart(): void {
    this.loading = true;
    this.error = false;
    this.service.chart().subscribe({
      next: response => { this.totalEmployees = response.data.totalEmployees; this.asOfDate = response.data.asOfDate; this.nodes = response.data.roots ?? []; this.loading = false; },
      error: () => { this.nodes = []; this.loading = false; this.error = true; }
    });
  }
}

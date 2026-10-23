import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { PlatformLoginFormComponent } from '../../features/platform/ui/platform-login-form/platform-login-form.component';
import { PlatformLoginUseCase } from '../../features/platform/application/platform-login.usecase';

@Component({
  selector: 'app-platform-login-page',
  standalone: true,
  imports: [CommonModule, PlatformLoginFormComponent],
  template: `
    <main class="hrcore-brand-theme min-h-screen bg-[radial-gradient(circle_at_top_left,var(--accent),transparent_42%),linear-gradient(135deg,var(--background),var(--muted))]">
      <div class="grid min-h-screen w-full lg:grid-cols-[1.08fr_0.92fr]">
        <aside class="relative hidden overflow-hidden bg-sidebar px-10 py-12 text-sidebar-foreground lg:flex lg:flex-col lg:justify-between xl:px-20 xl:py-16">
          <div class="pointer-events-none absolute -right-32 -top-32 h-96 w-96 rounded-full bg-secondary/20 blur-3xl"></div>
          <div class="pointer-events-none absolute -bottom-40 -left-32 h-[30rem] w-[30rem] rounded-full bg-primary/30 blur-3xl"></div>

          <div class="relative">
            <div class="flex items-center gap-4">
              <span class="flex h-16 w-16 items-center justify-center rounded-2xl bg-white p-2 shadow-xl shadow-black/10">
                <img src="/logo.png" alt="HR Core" class="h-full w-full object-contain">
              </span>
              <div>
                <p class="text-xl font-black tracking-tight">HR Core</p>
                <p class="mt-0.5 text-xs font-semibold uppercase tracking-[0.18em] text-sidebar-foreground/60">Plataforma</p>
              </div>
            </div>

            <div class="mt-20 max-w-xl xl:mt-28">
              <div class="flex h-44 w-44 items-center justify-center rounded-[2.75rem] bg-white/95 p-7 shadow-2xl shadow-black/20 xl:h-56 xl:w-56 xl:rounded-[3.5rem] xl:p-9">
                <img src="/logo.png" alt="Identidad HR Core" class="h-full w-full object-contain">
              </div>
              <p class="mt-12 text-xs font-black uppercase tracking-[0.24em] text-secondary">Consola de administración global</p>
              <h1 class="mt-5 text-5xl font-black leading-[1.05] tracking-tight xl:text-6xl">Todo HR Core bajo control.</h1>
              <p class="mt-6 max-w-lg text-base leading-8 text-sidebar-foreground/75">Gestiona organizaciones, planes y operaciones de la plataforma desde un espacio seguro, claro y centralizado.</p>
            </div>
          </div>

          <div class="relative flex items-center gap-3 text-xs font-semibold text-sidebar-foreground/65">
            <span class="h-2 w-2 rounded-full bg-secondary"></span>
            Acceso protegido para administradores globales
          </div>
        </aside>

        <section class="flex min-h-screen min-w-0 items-center justify-center bg-background/80 px-4 py-8 sm:px-8 lg:px-14 xl:px-24">
          <div class="w-full max-w-xl">
            <app-platform-login-form
              [embedded]="true"
              [status]="loginUseCase.status()"
              [error]="loginUseCase.error()"
              (loginSubmit)="onLogin($event)">
            </app-platform-login-form>
          </div>
        </section>
      </div>
    </main>
  `
})
export class PlatformLoginPageComponent {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
  protected loginUseCase = inject(PlatformLoginUseCase);

  async onLogin(credentials: { email: string; password: string }) {
    await this.loginUseCase.login(credentials);
    if (this.loginUseCase.status() === 'success') {
      const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
      const safeUrl = returnUrl && returnUrl.startsWith('/platform/') && !returnUrl.startsWith('//') && !returnUrl.includes('\\') ? returnUrl : '/platform/dashboard';
      this.router.navigateByUrl(safeUrl);
    }
  }
}

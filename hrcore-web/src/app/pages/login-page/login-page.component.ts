import { Component, inject, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { LoginUseCase, FaceLoginUseCase } from '../../features/auth';
import { LoginFormComponent } from '../../features/auth/ui/login-form/login-form.component';
import { FaceLoginRequest, LoginRequest } from '../../entities/auth';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { PendingCheckoutStorageService } from '../../entities/billing';

@Component({
  selector: 'app-login-page',
  standalone: true,
  imports: [CommonModule, RouterModule, LoginFormComponent],
  template: `
    <main class="hrcore-brand-theme min-h-screen bg-[radial-gradient(circle_at_top_left,var(--accent),transparent_42%),linear-gradient(135deg,var(--background),var(--muted))]">
      <div class="grid min-h-screen w-full lg:grid-cols-[0.85fr_1.15fr]">
        <aside class="relative hidden overflow-hidden bg-sidebar px-10 py-10 text-sidebar-foreground lg:flex lg:min-h-screen lg:flex-col xl:px-16 xl:py-12">
          <div class="absolute -right-24 -top-24 h-72 w-72 rounded-full bg-secondary/20 blur-3xl"></div>
          <div class="absolute -bottom-28 -left-24 h-80 w-80 rounded-full bg-primary/30 blur-3xl"></div>
          <div class="relative flex flex-1 flex-col justify-center pt-20">
            <a routerLink="/" class="absolute left-0 top-0 inline-flex items-center gap-3 rounded-2xl transition-opacity hover:opacity-85 focus:outline-none focus:ring-2 focus:ring-secondary/70 focus:ring-offset-2 focus:ring-offset-sidebar" aria-label="Volver al inicio" title="Volver al inicio">
              <span class="flex h-14 w-14 items-center justify-center rounded-2xl bg-white p-2 shadow-lg shadow-black/10"><img src="/logo.png" alt="HR Core" class="h-full w-full object-contain"></span>
              <span class="text-xl font-black tracking-tight">HR Core</span>
            </a>
            <div class="max-w-lg">
              <p class="text-xs font-black uppercase tracking-[0.22em] text-secondary">Gestión humana inteligente</p>
              <h1 class="mt-4 text-4xl font-black leading-[1.08] xl:text-5xl">Tu organización, siempre en movimiento.</h1>
              <p class="mt-5 max-w-md text-sm leading-7 text-sidebar-foreground/75">Accede a tus procesos laborales, asistencia y documentos desde un espacio seguro y centralizado.</p>
            </div>
          </div>
          <div class="relative space-y-3 pt-10 text-xs font-semibold text-sidebar-foreground/80">
            <div class="flex items-center gap-2"><span class="h-2 w-2 rounded-full bg-secondary"></span> Acceso seguro por organización</div>
            <div class="flex items-center gap-2"><span class="h-2 w-2 rounded-full bg-secondary"></span> Información laboral en un solo lugar</div>
          </div>
        </aside>

        <section class="flex min-w-0 items-center justify-center bg-background/90 px-5 py-10 sm:px-10 sm:py-12 lg:px-16 xl:px-24">
          <div class="w-full max-w-lg space-y-5">
            <div *ngIf="passwordResetSuccess" class="animate-slide-up rounded-2xl border border-primary/20 bg-card px-4 py-3 text-sm font-medium text-accent-foreground shadow-sm">
              Tu contraseña fue actualizada. Ahora puedes iniciar sesión con la nueva clave.
            </div>

            <app-login-form
              [embedded]="true"
              [status]="displayStatus()"
              [error]="displayError()"
              (loginSubmit)="handleLogin($event)"
              (faceLoginSubmit)="handleFaceLogin($event)"
            />
        </div>
        </section>
      </div>
    </main>
  `
})
export class LoginPageComponent implements OnInit {
  public readonly loginUseCase = inject(LoginUseCase);
  public readonly faceLoginUseCase = inject(FaceLoginUseCase);
  private readonly authFacade = inject(AuthFacade);
  private readonly pendingCheckoutStorage = inject(PendingCheckoutStorageService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  passwordResetSuccess = false;

  ngOnInit(): void {
    this.loginUseCase.resetState();
    this.faceLoginUseCase.resetState();
    this.pendingCheckoutStorage.clear();
    this.passwordResetSuccess = this.route.snapshot.queryParamMap.get('passwordReset') === 'success';
  }

  async handleLogin(request: LoginRequest): Promise<void> {
    this.faceLoginUseCase.resetState();
    await this.loginUseCase.login(request);

    if (this.loginUseCase.status() !== 'success') {
      return;
    }

    await this.authFacade.loadCurrentUser();
    await this.router.navigateByUrl(this.safeReturnUrl() ?? this.authFacade.getTenantLandingRoute(), {
      replaceUrl: true
    });
  }

  async handleFaceLogin(request: FaceLoginRequest): Promise<void> {
    this.loginUseCase.resetState();
    await this.faceLoginUseCase.login(request);

    if (this.faceLoginUseCase.status() !== 'success') {
      return;
    }

    await this.authFacade.loadCurrentUser();
    await this.router.navigateByUrl(this.safeReturnUrl() ?? this.authFacade.getTenantLandingRoute(), {
      replaceUrl: true
    });
  }

  private safeReturnUrl(): string | null {
    const value = this.route.snapshot.queryParamMap.get('returnUrl');
    if (!value || !value.startsWith('/dashboard/') || value.startsWith('//') || value.includes('\\')) return null;
    return value;
  }

  displayStatus(): 'idle' | 'loading' | 'success' | 'error' {
    if (this.loginUseCase.status() === 'loading' || this.faceLoginUseCase.status() === 'loading') {
      return 'loading';
    }

    if (this.loginUseCase.status() === 'error' || this.faceLoginUseCase.status() === 'error') {
      return 'error';
    }

    if (this.loginUseCase.status() === 'success' || this.faceLoginUseCase.status() === 'success') {
      return 'success';
    }

    return 'idle';
  }

  displayError(): string | null {
    return this.faceLoginUseCase.error() || this.loginUseCase.error();
  }
}

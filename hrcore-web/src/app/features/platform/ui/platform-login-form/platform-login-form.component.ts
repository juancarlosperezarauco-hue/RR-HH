import { Component, EventEmitter, Input, Output, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LucideAngularModule } from 'lucide-angular';

export interface PlatformLoginRequest {
  email: string;
  password: string;
}

@Component({
  selector: 'app-platform-login-form',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule, LucideAngularModule],
  template: `
    <div [ngClass]="embedded ? 'w-full' : 'hrcore-brand-theme flex min-h-screen items-center justify-center bg-[radial-gradient(circle_at_top_right,var(--accent),transparent_38%),linear-gradient(135deg,var(--background),var(--muted))] p-3 sm:p-6'">
      <div class="w-full max-w-lg">
        <div class="mb-7 text-center lg:mb-8">
          <img *ngIf="!embedded"
            src="/logo.png"
            alt="HR Core"
            class="mx-auto h-16 w-16 rounded-2xl bg-white p-2 object-contain shadow-lg"
          >
          <p class="mt-4 text-xs font-black uppercase tracking-[0.2em] text-primary">Consola de plataforma</p>
          <h2 class="mt-2 text-3xl font-black tracking-tight text-foreground sm:text-4xl">Bienvenido de vuelta</h2>
          <p class="mt-2 text-sm leading-6 text-muted-foreground">Acceso exclusivo para administradores globales</p>
        </div>

        <div [ngClass]="embedded ? 'rounded-[28px] p-0' : 'rounded-[28px] border border-border/80 bg-card p-6 shadow-2xl sm:p-9'">
          <!-- Mensaje de Error -->
          <div *ngIf="status === 'error'" class="mb-6 flex items-start gap-2.5 rounded-2xl border border-destructive/25 bg-destructive/10 p-3.5 text-sm font-medium text-destructive" role="alert">
            <lucide-icon name="circle-alert" class="mt-0.5 h-4 w-4 shrink-0"></lucide-icon>
            {{ error }}
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-4">
            
            <div>
              <div class="flex justify-between items-center mb-1">
                <label for="email" class="block text-xs font-black uppercase tracking-wider text-muted-foreground">Correo electrónico</label>
                <!-- ✅ Botón para auto-llenar credenciales -->
                <button 
                  type="button"
                  (click)="autoFillCredentials()"
                  class="inline-flex cursor-pointer items-center gap-1.5 rounded-lg px-2 py-1 text-xs font-bold text-primary transition-colors hover:bg-accent"
                  title="Auto-llenar credenciales de superadmin">
                  <lucide-icon name="wand-2" class="h-3 w-3"></lucide-icon>
                  Auto-llenar
                </button>
              </div>
              <input 
                id="email" 
                type="email" 
                formControlName="email"
                autocomplete="username"
                class="h-12 w-full rounded-xl border border-input bg-background px-3.5 text-sm text-foreground outline-none transition-all placeholder:text-muted-foreground/70 hover:border-primary/40 focus:border-primary focus:ring-2 focus:ring-primary/15"
                [class.border-destructive]="isFieldInvalid('email')"
                placeholder="superadmin@hrcore.local">
              <span *ngIf="isFieldInvalid('email')" class="text-xs text-destructive mt-1">Ingresa un correo válido.</span>
            </div>

            <div>
              <label for="password" class="mb-1.5 block text-xs font-black uppercase tracking-wider text-muted-foreground">Contraseña</label>
              <div class="relative">
                <input 
                  id="password" 
                  [type]="showPassword ? 'text' : 'password'" 
                  formControlName="password"
                  autocomplete="current-password"
                  class="h-12 w-full rounded-xl border border-input bg-background px-3.5 pr-11 text-sm text-foreground outline-none transition-all placeholder:text-muted-foreground/70 hover:border-primary/40 focus:border-primary focus:ring-2 focus:ring-primary/15"
                  [class.border-destructive]="isFieldInvalid('password')"
                  placeholder="••••••••">
                <button 
                  type="button" 
                  (click)="togglePasswordVisibility()" 
                  class="absolute inset-y-0 right-0 flex items-center px-3.5 text-muted-foreground transition-colors hover:text-primary"
                  tabindex="-1"
                >
                  <lucide-icon [name]="showPassword ? 'eye-off' : 'eye'" class="h-4 w-4"></lucide-icon>
                </button>
              </div>
              <span *ngIf="isFieldInvalid('password')" class="text-xs text-destructive mt-1">La contraseña es requerida (mínimo 8 caracteres).</span>
            </div>

            <div class="pt-4">
              <button 
                type="submit" 
                [disabled]="status === 'loading'"
                class="flex h-12 w-full items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground shadow-sm transition-all hover:-translate-y-0.5 hover:bg-primary/90 hover:shadow-md disabled:cursor-not-allowed disabled:opacity-50">
                
                <ng-container *ngIf="status !== 'loading'; else loadingSpinner">
                  Acceder a la consola
                  <lucide-icon name="arrow-right" class="h-4 w-4"></lucide-icon>
                </ng-container>
                
                <ng-template #loadingSpinner>
                  <svg class="animate-spin h-5 w-5 mr-3" viewBox="0 0 24 24">
                    <circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"></circle>
                    <path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                  </svg>
                  Autenticando...
                </ng-template>

              </button>
            </div>
            
            <!-- Link to return to landing -->
            <div class="mt-6 border-t border-border pt-5 text-center text-sm text-muted-foreground">
              <a routerLink="/" class="font-bold text-primary transition-colors hover:underline">
                ← Volver al inicio
              </a>
            </div>
          </form>
        </div>
      </div>
    </div>
  `
})
export class PlatformLoginFormComponent {
  @Input() status: 'idle' | 'loading' | 'success' | 'error' = 'idle';
  @Input() error: string | null = null;
  @Input() embedded = false;
  @Output() loginSubmit = new EventEmitter<PlatformLoginRequest>();

  showPassword = false;
  private readonly fb = inject(FormBuilder);

  loginForm = this.fb.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  // ✅ Método para auto-llenar credenciales de superadmin
  autoFillCredentials(): void {
    this.loginForm.patchValue({
      email: 'superadmin@hrcore.local',
      password: 'SuperAdmin123!'
    });
    // Marcar campos como tocados para que no muestren errores de validación
    this.loginForm.get('email')?.markAsTouched();
    this.loginForm.get('password')?.markAsTouched();
  }

  onSubmit(): void {
    if (this.loginForm.valid && this.status !== 'loading') {
      this.loginSubmit.emit(this.loginForm.value as PlatformLoginRequest);
    } else {
      this.loginForm.markAllAsTouched();
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.loginForm.get(fieldName);
    return field ? field.invalid && (field.dirty || field.touched) : false;
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }
}

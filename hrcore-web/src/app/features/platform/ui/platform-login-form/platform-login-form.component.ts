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
    <div class="hrcore-brand-theme min-h-screen bg-card flex flex-col justify-center py-12 sm:px-6 lg:px-8">
      <div class="sm:mx-auto sm:w-full sm:max-w-md">
        <div class="text-center">
          <img
            src="/logo.png"
            alt="Prospera"
            class="mx-auto h-12 w-12 rounded-xl object-contain"
          >
          <h2 class="mt-6 text-3xl font-extrabold text-accent-foreground">Plataforma</h2>
          <p class="mt-2 text-sm text-muted-foreground">Acceso exclusivo para administradores globales</p>
        </div>

        <div class="mt-8 bg-card py-8 px-4 shadow-xl rounded-lg border border-border sm:px-10">
          <!-- Mensaje de Error -->
          <div *ngIf="status === 'error'" class="mb-6 p-4 bg-destructive/5 text-destructive border border-destructive/20 rounded-md">
            {{ error }}
          </div>

          <form [formGroup]="loginForm" (ngSubmit)="onSubmit()" class="space-y-4">
            
            <div>
              <div class="flex justify-between items-center mb-1">
                <label for="email" class="block text-sm font-medium text-primary">Correo Electrónico</label>
                <!-- ✅ Botón para auto-llenar credenciales -->
                <button 
                  type="button"
                  (click)="autoFillCredentials()"
                  class="text-xs text-secondary hover:text-primary transition-colors flex items-center gap-1 cursor-pointer"
                  title="Auto-llenar credenciales de superadmin">
                  <lucide-icon name="wand-2" class="h-3 w-3"></lucide-icon>
                  Auto-llenar
                </button>
              </div>
              <input 
                id="email" 
                type="email" 
                formControlName="email"
                class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-colors"
                [class.border-destructive/30]="isFieldInvalid('email')"
                placeholder="superadmin@hrcore.local">
              <span *ngIf="isFieldInvalid('email')" class="text-xs text-destructive mt-1">Ingresa un correo válido.</span>
            </div>

            <div>
              <label for="password" class="block text-sm font-medium text-primary mb-1">Contraseña</label>
              <div class="relative">
                <input 
                  id="password" 
                  [type]="showPassword ? 'text' : 'password'" 
                  formControlName="password"
                  class="w-full px-3 py-2 border border-border rounded-md focus:outline-none focus:ring-2 focus:ring-primary focus:border-transparent transition-colors pr-10"
                  [class.border-destructive/30]="isFieldInvalid('password')"
                  placeholder="••••••••">
                <button 
                  type="button" 
                  (click)="togglePasswordVisibility()" 
                  class="absolute inset-y-0 right-0 pr-3 flex items-center text-muted-foreground hover:text-primary transition-colors"
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
                class="w-full bg-accent-foreground hover:bg-primary text-primary-foreground font-medium py-2.5 px-4 rounded-md transition-colors disabled:opacity-50 flex justify-center items-center">
                
                <ng-container *ngIf="status !== 'loading'; else loadingSpinner">
                  Acceder como SuperAdmin
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
            <div class="mt-6 text-center text-sm text-muted-foreground border-t border-border pt-4">
              <a routerLink="/" class="font-medium text-secondary hover:text-primary transition-colors">
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

import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { PlatformLoginFormComponent } from '../../features/platform/ui/platform-login-form/platform-login-form.component';
import { PlatformLoginUseCase } from '../../features/platform/application/platform-login.usecase';

@Component({
  selector: 'app-platform-login-page',
  standalone: true,
  imports: [CommonModule, PlatformLoginFormComponent],
  template: `
    <app-platform-login-form 
      [status]="loginUseCase.status()"
      [error]="loginUseCase.error()"
      (loginSubmit)="onLogin($event)">
    </app-platform-login-form>
  `
})
export class PlatformLoginPageComponent {
  private router = inject(Router);
  protected loginUseCase = inject(PlatformLoginUseCase);

  async onLogin(credentials: { email: string; password: string }) {
    await this.loginUseCase.login(credentials);
    if (this.loginUseCase.status() === 'success') {
      this.router.navigate(['/platform/dashboard']);
    }
  }
}
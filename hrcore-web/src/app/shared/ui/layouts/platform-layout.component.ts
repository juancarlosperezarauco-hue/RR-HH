// shared/ui/layouts/platform-layout.component.ts
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../../../widgets/layoutAdmin/sidebar/sidebar.component';
import { HeaderComponent } from '../../../widgets/layoutAdmin/header/header.component';
import { PlatformLogoutUseCase } from '../../../features/platform/application/platform-logout.usecase';

@Component({
  selector: 'app-platform-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, HeaderComponent],
  template: `
    <div class="flex h-screen overflow-hidden bg-[#F1F8E9]">
      <app-sidebar (logoutAction)="onLogout()"></app-sidebar>
      
      <div class="flex-1 flex flex-col overflow-hidden">
        <app-header (logoutAction)="onLogout()"></app-header>
        
        <main class="flex-1 overflow-y-auto p-6">
          <router-outlet></router-outlet>
        </main>
      </div>
    </div>
  `
})
export class PlatformLayoutComponent {
  private router = inject(Router);
  private platformLogoutUseCase = inject(PlatformLogoutUseCase);

  async onLogout(): Promise<void> {
    await this.platformLogoutUseCase.logout();
    this.router.navigate(['/platform/login']);
  }
}

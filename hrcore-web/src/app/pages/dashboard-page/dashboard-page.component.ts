import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, RouterOutlet } from '@angular/router';
import { AuthFacade } from '../../shared/lib/auth/auth.facade';
import { SidebarComponent, HeaderComponent } from '../../widgets/layoutAdmin';

@Component({
  selector: 'app-dashboard-page',
  standalone: true,
  imports: [CommonModule, RouterModule, RouterOutlet, SidebarComponent, HeaderComponent],

  template: `
    <div class="flex h-screen w-full overflow-hidden bg-[#F1F8E9]">
      <app-sidebar class="w-0 md:w-64 shrink-0 transition-all duration-300 relative z-50" (logoutAction)="logout()"></app-sidebar>

      <main class="relative flex min-w-0 flex-1 flex-col overflow-hidden">
        <app-header (logoutAction)="logout()"></app-header>

        <div class="flex-1 overflow-y-auto p-4 sm:p-6 lg:p-8">
          <router-outlet></router-outlet>
        </div>
      </main>
    </div>
  `
})
export class DashboardPageComponent {
  private readonly authFacade = inject(AuthFacade);

  logout(): void {
    this.authFacade.logout();
  }
}

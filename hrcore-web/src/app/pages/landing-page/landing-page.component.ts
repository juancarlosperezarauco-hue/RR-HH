import { Component } from '@angular/core';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';
import { LandingPageWidgetComponent } from '../../widgets/landing';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [PublicNavbarComponent, LandingPageWidgetComponent, PublicFooterComponent],
  template: `
    <div class="min-h-screen bg-white">
      <app-public-navbar />
      <app-landing-page-widget />
      <app-public-footer />
    </div>
  `
})
export class LandingPageComponent {}

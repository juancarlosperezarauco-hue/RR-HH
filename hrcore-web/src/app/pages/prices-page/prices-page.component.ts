import { Component } from '@angular/core';
import { PublicFooterComponent, PublicNavbarComponent } from '../../shared/ui/public-layout';
import { PricingPageWidgetComponent } from '../../widgets/pricing';

@Component({
  selector: 'app-prices-page',
  standalone: true,
  imports: [PublicNavbarComponent, PricingPageWidgetComponent, PublicFooterComponent],
  template: `
    <div class="min-h-screen bg-[#F7FBF3]">
      <app-public-navbar />
      <app-pricing-page-widget />
      <app-public-footer />
    </div>
  `
})
export class PricesPageComponent {}

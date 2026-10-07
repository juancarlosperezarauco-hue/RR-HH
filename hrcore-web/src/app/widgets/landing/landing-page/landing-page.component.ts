import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';
import { LandingBenefitsComponent } from '../landing-benefits/landing-benefits.component';
import { LandingHeroComponent } from '../landing-hero/landing-hero.component';
import { LandingMobileExperienceComponent } from '../landing-mobile-experience/landing-mobile-experience.component';
import { LandingMobileScreenshotsComponent } from '../landing-mobile-screenshots/landing-mobile-screenshots.component';
import { LandingPricingPreviewComponent } from '../landing-pricing-preview/landing-pricing-preview.component';
import { LandingTutorialsComponent } from '../landing-tutorials/landing-tutorials.component';
import { LandingWebPlatformComponent } from '../landing-web-platform/landing-web-platform.component';

@Component({
  selector: 'app-landing-page-widget',
  standalone: true,
  imports: [
    CommonModule,
    LandingHeroComponent,
    LandingBenefitsComponent,
    LandingTutorialsComponent,
    LandingWebPlatformComponent,
    LandingMobileScreenshotsComponent,
    LandingMobileExperienceComponent,
    LandingPricingPreviewComponent
  ],
  template: `
    <main class="min-h-screen bg-white text-[#101827]">
      <app-landing-hero />

      <app-landing-benefits />

      <!-- <app-landing-tutorials /> -->

      <!-- <app-landing-web-platform /> -->

      <!-- <app-landing-mobile-screenshots /> -->

      <!-- <app-landing-mobile-experience /> -->

      <app-landing-pricing-preview />
    </main>
  `
})
export class LandingPageWidgetComponent {}

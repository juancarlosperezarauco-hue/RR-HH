import { CommonModule } from '@angular/common';
import { Component, Input } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

@Component({
  selector: 'app-landing-placeholder-section',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section
      [id]="sectionId"
      class="border-t border-[#E0EEDF] bg-white px-4 py-20 sm:px-6 lg:px-8">

      <div class="mx-auto max-w-7xl">
        <div class="mx-auto max-w-3xl text-center">
          <div class="mx-auto mb-5 inline-flex w-fit items-center gap-2 rounded-full bg-[#E8F5E9] px-5 py-2 text-sm font-black text-[#2E7D32]">
            <lucide-icon [name]="icon" class="h-4 w-4"></lucide-icon>
            {{ badge }}
          </div>

          <h2 class="text-4xl font-black tracking-tight text-[#101827] sm:text-5xl">
            {{ title }}
          </h2>

          <p class="mt-5 text-base leading-8 text-[#405447]">
            {{ description }}
          </p>
        </div>

        <div class="mt-12 grid gap-6 md:grid-cols-3">
          <article class="rounded-[28px] border border-[#DDEED8] bg-[#FAFCF8] p-7 shadow-sm">
            <div class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
              <lucide-icon name="check-circle" class="h-5 w-5"></lucide-icon>
            </div>
            <h3 class="text-lg font-black text-[#101827]">Placeholder 01</h3>
            <p class="mt-3 text-sm leading-7 text-[#405447]">
              Esta sección queda reservada para implementar el contenido final.
            </p>
          </article>

          <article class="rounded-[28px] border border-[#DDEED8] bg-[#FAFCF8] p-7 shadow-sm">
            <div class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
              <lucide-icon name="check-circle" class="h-5 w-5"></lucide-icon>
            </div>
            <h3 class="text-lg font-black text-[#101827]">Placeholder 02</h3>
            <p class="mt-3 text-sm leading-7 text-[#405447]">
              Mantiene estructura visual sin volver a usar la landing anterior.
            </p>
          </article>

          <article class="rounded-[28px] border border-[#DDEED8] bg-[#FAFCF8] p-7 shadow-sm">
            <div class="mb-5 flex h-12 w-12 items-center justify-center rounded-2xl bg-[#E8F5E9] text-[#2E7D32]">
              <lucide-icon name="check-circle" class="h-5 w-5"></lucide-icon>
            </div>
            <h3 class="text-lg font-black text-[#101827]">Placeholder 03</h3>
            <p class="mt-3 text-sm leading-7 text-[#405447]">
              Luego reemplazamos esto por la sección profesional real.
            </p>
          </article>
        </div>
      </div>
    </section>
  `
})
export class LandingPlaceholderSectionComponent {
  @Input({ required: true }) sectionId = '';
  @Input({ required: true }) badge = '';
  @Input({ required: true }) title = '';
  @Input({ required: true }) description = '';
  @Input() icon = 'sparkles';
}

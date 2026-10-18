import { Component, input } from '@angular/core';
import { ManualSection } from './manual-content';

@Component({
  selector: 'app-manual-section',
  standalone: true,
  template: `
    <article [id]="section().id" class="scroll-mt-28 rounded-[28px] border border-border bg-card p-6 shadow-sm sm:p-8">
      <p class="text-xs font-black uppercase tracking-[0.18em] text-primary">{{ section().eyebrow }}</p>
      <h2 class="mt-3 text-2xl font-black tracking-tight text-accent-foreground sm:text-3xl">{{ section().title }}</h2>
      <p class="mt-3 max-w-3xl text-base leading-7 text-muted-foreground">{{ section().summary }}</p>

      <div class="mt-8 grid gap-6">
        @for (topic of section().topics; track topic.title) {
          <section class="border-t border-border pt-5 first:border-t-0 first:pt-0">
            <h3 class="text-base font-black text-accent-foreground">{{ topic.title }}</h3>
            @for (paragraph of topic.paragraphs; track paragraph) {
              <p class="mt-2 text-sm leading-7 text-foreground">{{ paragraph }}</p>
            }
            @if (topic.bullets?.length) {
              <ul class="mt-3 grid gap-2 pl-5 text-sm leading-6 text-muted-foreground marker:text-primary">
                @for (bullet of topic.bullets; track bullet) {
                  <li class="list-disc">{{ bullet }}</li>
                }
              </ul>
            }
          </section>
        }
      </div>
    </article>
  `
})
export class ManualSectionComponent {
  readonly section = input.required<ManualSection>();
}

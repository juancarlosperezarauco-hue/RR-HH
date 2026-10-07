import { CommonModule } from '@angular/common';
import { Component, signal } from '@angular/core';
import { LucideAngularModule } from 'lucide-angular';

interface PricingFaqItem {
  question: string;
  answer: string;
}

@Component({
  selector: 'app-pricing-faq',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <section class="bg-white px-4 py-20 sm:px-6 lg:px-8">
      <div class="mx-auto max-w-4xl">
        <div class="mx-auto max-w-3xl text-center">
          <div class="mx-auto mb-5 inline-flex w-fit items-center gap-2 rounded-full bg-[#E8F5E9] px-5 py-2 text-sm font-black text-[#2E7D32]">
            <lucide-icon name="circle-help" class="h-4 w-4"></lucide-icon>
            Preguntas frecuentes
          </div>

          <h2 class="text-4xl font-black tracking-tight text-[#101827] sm:text-5xl">Dudas comunes antes de elegir un plan</h2>
        </div>

        <div class="mt-12 space-y-4">
          @for (item of faqs; track item.question; let index = $index) {
            <article class="rounded-[24px] border border-[#DDEED8] bg-[#FAFCF8]">
              <button type="button" (click)="toggle(index)" class="flex w-full items-center justify-between gap-5 p-5 text-left">
                <span class="font-black text-[#101827]">{{ item.question }}</span>
                <lucide-icon [name]="openIndex() === index ? 'minus' : 'plus'" class="h-5 w-5 text-[#2E7D32]"></lucide-icon>
              </button>

              @if (openIndex() === index) {
                <div class="border-t border-[#DDEED8] px-5 pb-5 pt-4 text-sm leading-7 text-[#405447]">
                  {{ item.answer }}
                </div>
              }
            </article>
          }
        </div>
      </div>
    </section>
  `
})
export class PricingFaqComponent {
  readonly openIndex = signal(0);

  readonly faqs: PricingFaqItem[] = [
    {
      question: '¿Puedo empezar gratis?',
      answer: 'Sí. Puedes crear un tenant con plan DEMO para validar el flujo principal antes de contratar un plan pagado.'
    },
    {
      question: '¿El pago se realiza con Stripe?',
      answer: 'Sí. Los planes pagados redirigen a Stripe Checkout y el backend activa la suscripción mediante webhook.'
    },
    {
      question: '¿Puedo cambiar de plan después?',
      answer: 'Sí. El tenant puede mejorar su plan desde su sesión cuando necesite más capacidad.'
    },
    {
      question: '¿El plan Enterprise usa checkout automático?',
      answer: 'No. Enterprise está pensado para acuerdos comerciales personalizados, por eso requiere contacto comercial.'
    }
  ];

  toggle(index: number): void {
    this.openIndex.set(this.openIndex() === index ? -1 : index);
  }
}

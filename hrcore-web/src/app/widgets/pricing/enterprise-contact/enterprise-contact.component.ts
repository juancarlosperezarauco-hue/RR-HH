import { CommonModule } from '@angular/common';
import { Component, inject, Input } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { LucideAngularModule } from 'lucide-angular';
import { PublicBillingService } from '../../../entities/billing';
import { ToastService } from '../../../shared/ui/toast/toast.service';

@Component({
  selector: 'app-enterprise-contact',
  standalone: true,
  imports: [CommonModule, FormsModule, LucideAngularModule],
  template: `
    <button
      type="button"
      class="inline-flex h-11 w-full items-center justify-center gap-2 rounded-xl border border-border bg-card px-5 text-sm font-black text-accent-foreground transition-colors hover:bg-background"
      (click)="open()">
      {{ buttonLabel }}
      <lucide-icon name="send" class="h-4 w-4"></lucide-icon>
    </button>

    @if (visible) {
      <div class="fixed inset-0 z-[100] flex items-center justify-center bg-sidebar/60 p-4" role="presentation" (click)="close()">
        <section
          role="dialog"
          aria-modal="true"
          aria-labelledby="enterprise-contact-title"
          class="max-h-[90vh] w-full max-w-xl overflow-y-auto rounded-3xl border border-border bg-card p-6 shadow-2xl sm:p-8"
          (click)="$event.stopPropagation()">
          <div class="flex items-start justify-between gap-4">
            <div>
              <p class="text-xs font-black uppercase tracking-[0.18em] text-primary">Enterprise</p>
              <h2 id="enterprise-contact-title" class="mt-2 text-2xl font-black text-foreground">Solicita una propuesta a medida</h2>
              <p class="mt-2 text-sm leading-6 text-muted-foreground">Cuéntanos sobre tu organización y nuestro equipo comercial se pondrá en contacto contigo.</p>
            </div>
            <button type="button" aria-label="Cerrar" class="rounded-lg p-2 text-muted-foreground hover:bg-accent hover:text-foreground" (click)="close()">
              <lucide-icon name="x" class="h-5 w-5"></lucide-icon>
            </button>
          </div>

          <form class="mt-7 space-y-4" (ngSubmit)="submit()" #form="ngForm">
            <div class="grid gap-4 sm:grid-cols-2">
              <label class="space-y-1.5 text-sm font-semibold text-foreground">
                Nombre completo
                <input name="fullName" [(ngModel)]="request.fullName" required maxlength="120" class="h-11 w-full rounded-xl border border-border bg-background px-3 text-sm font-normal outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" placeholder="Tu nombre y apellido">
              </label>
              <label class="space-y-1.5 text-sm font-semibold text-foreground">
                Correo corporativo
                <input type="email" name="email" [(ngModel)]="request.email" required maxlength="180" email class="h-11 w-full rounded-xl border border-border bg-background px-3 text-sm font-normal outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" placeholder="nombre@empresa.com">
              </label>
            </div>
            <div class="grid gap-4 sm:grid-cols-2">
              <label class="space-y-1.5 text-sm font-semibold text-foreground">
                Empresa
                <input name="company" [(ngModel)]="request.company" required maxlength="160" class="h-11 w-full rounded-xl border border-border bg-background px-3 text-sm font-normal outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" placeholder="Nombre de la empresa">
              </label>
              <label class="space-y-1.5 text-sm font-semibold text-foreground">
                Tamaño del equipo
                <select name="teamSize" [(ngModel)]="request.teamSize" required class="h-11 w-full rounded-xl border border-border bg-background px-3 text-sm font-normal outline-none focus:border-primary focus:ring-2 focus:ring-primary/20">
                  <option value="" disabled>Selecciona una opción</option>
                  <option value="1-25">1–25 personas</option>
                  <option value="26-100">26–100 personas</option>
                  <option value="101-500">101–500 personas</option>
                  <option value="501+">Más de 500 personas</option>
                </select>
              </label>
            </div>
            <label class="block space-y-1.5 text-sm font-semibold text-foreground">
              ¿Qué necesita tu organización?
              <textarea name="message" [(ngModel)]="request.message" required minlength="20" maxlength="2000" rows="4" class="w-full resize-y rounded-xl border border-border bg-background px-3 py-3 text-sm font-normal outline-none focus:border-primary focus:ring-2 focus:ring-primary/20" placeholder="Describe brevemente tus necesidades de gestión de personas"></textarea>
            </label>

            <div class="flex flex-col-reverse gap-3 pt-2 sm:flex-row sm:justify-end">
              <button type="button" class="h-11 rounded-xl border border-border px-5 text-sm font-bold text-foreground hover:bg-accent" (click)="close()">Cancelar</button>
              <button type="submit" [disabled]="form.invalid || sending" class="inline-flex h-11 items-center justify-center gap-2 rounded-xl bg-primary px-5 text-sm font-black text-primary-foreground hover:bg-primary/90 disabled:cursor-not-allowed disabled:opacity-60">
                @if (sending) { <lucide-icon name="loader-circle" class="h-4 w-4 animate-spin"></lucide-icon> }
                {{ sending ? 'Enviando solicitud...' : 'Enviar solicitud' }}
              </button>
            </div>
          </form>
        </section>
      </div>
    }
  `
})
export class EnterpriseContactComponent {
  @Input() buttonLabel = 'Solicitar propuesta';

  private readonly billingService = inject(PublicBillingService);
  private readonly toastService = inject(ToastService);

  visible = false;
  sending = false;
  request = { fullName: '', email: '', company: '', teamSize: '', message: '' };

  open(): void { this.visible = true; }

  close(): void {
    if (!this.sending) this.visible = false;
  }

  submit(): void {
    if (this.sending) return;
    this.sending = true;
    this.billingService.sendEnterpriseInquiry(this.request).subscribe({
      next: (response) => {
        if (!response.success) {
          this.toastService.error(response.message || 'No se pudo enviar la solicitud.');
          return;
        }
        this.toastService.success(response.message || 'Solicitud Enterprise enviada correctamente.');
        this.request = { fullName: '', email: '', company: '', teamSize: '', message: '' };
        this.visible = false;
      },
      error: (error: { error?: { message?: string } }) => {
        this.toastService.error(error.error?.message || 'No se pudo enviar la solicitud. Inténtalo nuevamente.');
        this.sending = false;
      },
      complete: () => { this.sending = false; }
    });
  }
}

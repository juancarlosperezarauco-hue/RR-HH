import { Injectable, signal } from '@angular/core';
import { ConfirmModalOptions } from './confirm-modal.model';

@Injectable({
  providedIn: 'root'
})
export class ConfirmModalService {
  readonly isOpen = signal<boolean>(false);
  readonly options = signal<ConfirmModalOptions>({
    title: '',
    message: '',
    confirmText: 'Aceptar',
    cancelText: 'Cancelar',
    variant: 'danger'
  });

  private resolver: ((value: boolean) => void) | null = null;

  confirm(options: ConfirmModalOptions): Promise<boolean> {
    if (this.resolver) {
      this.resolver(false);
      this.resolver = null;
    }

    this.options.set({
      confirmText: options.confirmText ?? 'Aceptar',
      cancelText: options.cancelText ?? 'Cancelar',
      variant: options.variant ?? 'danger',
      ...options
    });
    this.isOpen.set(true);

    if (typeof document !== 'undefined') {
      document.body.style.overflow = 'hidden';
    }

    return new Promise<boolean>((resolve) => {
      this.resolver = resolve;
    });
  }

  handleConfirm(): void {
    if (this.resolver) {
      this.resolver(true);
      this.resolver = null;
    }
    this.close();
  }

  handleCancel(): void {
    if (this.resolver) {
      this.resolver(false);
      this.resolver = null;
    }
    this.close();
  }

  private close(): void {
    this.isOpen.set(false);
    if (typeof document !== 'undefined') {
      document.body.style.overflow = '';
    }
  }
}

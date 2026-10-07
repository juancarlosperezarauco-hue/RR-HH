import { Injectable, signal } from '@angular/core';

export type ToastType = 'success' | 'error' | 'warning' | 'info';

export interface ToastMessage {
  id: string;
  type: ToastType;
  title: string;
  message: string;
  duration?: number;
}

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  readonly toasts = signal<ToastMessage[]>([]);

  show(type: ToastType, title: string, message: string, duration: number = 5000): void {
    const id = Math.random().toString(36).substring(2, 9);
    const toast: ToastMessage = { id, type, title, message, duration };
    
    this.toasts.update(current => [...current, toast]);

    if (duration > 0) {
      setTimeout(() => this.remove(id), duration);
    }
  }

  success(message: string, title: string = 'Éxito'): void {
    this.show('success', title, message);
  }

  error(message: string, title: string = 'Error'): void {
    this.show('error', title, message, 7000); // Errors stay a bit longer
  }

  warning(message: string, title: string = 'Advertencia'): void {
    this.show('warning', title, message);
  }

  info(message: string, title: string = 'Información'): void {
    this.show('info', title, message);
  }

  remove(id: string): void {
    this.toasts.update(current => current.filter(t => t.id !== id));
  }
}

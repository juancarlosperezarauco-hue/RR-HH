export type ConfirmVariant = 'danger' | 'warning' | 'info' | 'success';

export interface ConfirmModalOptions {
  title: string;
  message: string;
  details?: string;
  confirmText?: string;
  cancelText?: string;
  variant?: ConfirmVariant;
  icon?: string;
}

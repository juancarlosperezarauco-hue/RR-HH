import { Injectable } from '@angular/core';
import { PendingCheckout } from '../model/pending-checkout.model';

@Injectable({
  providedIn: 'root'
})
export class PendingCheckoutStorageService {
  private readonly KEY = 'hrcore.checkout.pending';

  save(data: PendingCheckout): void {
    localStorage.setItem(this.KEY, JSON.stringify(data));
  }

  get(): PendingCheckout | null {
    const raw = localStorage.getItem(this.KEY);

    if (!raw) {
      return null;
    }

    try {
      return JSON.parse(raw) as PendingCheckout;
    } catch {
      this.clear();
      return null;
    }
  }

  clear(): void {
    localStorage.removeItem(this.KEY);
  }
}

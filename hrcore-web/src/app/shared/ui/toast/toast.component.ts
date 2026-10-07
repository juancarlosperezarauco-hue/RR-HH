import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ToastService, ToastMessage } from './toast.service';
import { LucideAngularModule, CheckCircle, AlertCircle, AlertTriangle, Info, X } from 'lucide-angular';

@Component({
  selector: 'app-toast',
  standalone: true,
  imports: [CommonModule, LucideAngularModule],
  template: `
    <div class="fixed top-4 right-4 z-[9999] flex flex-col gap-3 w-full max-w-sm pointer-events-none">
      @for (toast of toastService.toasts(); track toast.id) {
        <div 
          class="pointer-events-auto flex w-full flex-col overflow-hidden rounded-xl bg-card border shadow-lg transition-all duration-300 transform translate-y-0 opacity-100"
          [ngClass]="{
            'border-green-500/30': toast.type === 'success',
            'border-red-500/30': toast.type === 'error',
            'border-yellow-500/30': toast.type === 'warning',
            'border-blue-500/30': toast.type === 'info'
          }">
          
          <div class="p-4 flex items-start gap-3">
            <div class="flex-shrink-0 mt-0.5">
              @switch (toast.type) {
                @case ('success') { <lucide-icon name="check-circle" [size]="20" class="text-green-500"></lucide-icon> }
                @case ('error') { <lucide-icon name="alert-circle" [size]="20" class="text-red-500"></lucide-icon> }
                @case ('warning') { <lucide-icon name="alert-triangle" [size]="20" class="text-yellow-500"></lucide-icon> }
                @case ('info') { <lucide-icon name="info" [size]="20" class="text-blue-500"></lucide-icon> }
              }
            </div>
            
            <div class="flex-1 w-0">
              <p class="text-sm font-semibold text-foreground">
                {{ toast.title }}
              </p>
              <p class="mt-1 text-sm text-muted-foreground break-words">
                {{ toast.message }}
              </p>
            </div>
            
            <button 
              (click)="toastService.remove(toast.id)"
              class="flex-shrink-0 ml-4 p-1 rounded-md inline-flex text-muted-foreground hover:bg-muted focus:outline-none transition-colors">
              <lucide-icon name="x" [size]="16"></lucide-icon>
            </button>
          </div>
          
          <!-- Progress bar -->
          <div class="h-1 w-full bg-muted/50">
            <div class="h-full origin-left animate-progress"
              [ngClass]="{
                'bg-green-500': toast.type === 'success',
                'bg-red-500': toast.type === 'error',
                'bg-yellow-500': toast.type === 'warning',
                'bg-blue-500': toast.type === 'info'
              }"
              [style.animation-duration]="toast.duration + 'ms'">
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    @keyframes progress {
      0% { transform: scaleX(1); }
      100% { transform: scaleX(0); }
    }
    .animate-progress {
      animation-name: progress;
      animation-timing-function: linear;
      animation-fill-mode: forwards;
    }
  `]
})
export class ToastComponent {
  public toastService = inject(ToastService);
}

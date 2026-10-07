import { Component, inject } from '@angular/core';
import { ToastService } from '../../core/services/toast.service';
import { AppIcon } from './app-icon';

@Component({
  selector: 'app-toast-outlet',
  imports: [AppIcon],
  template: `
    <div class="toast-stack" aria-live="polite" aria-atomic="true">
      @for (toast of toasts.items(); track toast.id) {
        <div [class]="'toast toast--' + toast.kind" role="status">
          <span class="toast__mark" aria-hidden="true"></span>
          <span>{{ toast.message }}</span>
          <button class="icon-button" type="button" aria-label="Dismiss notification" (click)="toasts.dismiss(toast.id)"><app-icon name="close" /></button>
        </div>
      }
    </div>
  `,
})
export class ToastOutlet {
  readonly toasts = inject(ToastService);
}

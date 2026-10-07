import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ThemeService } from './core/services/theme.service';
import { ToastOutlet } from './shared/ui/toast-outlet';

@Component({
  imports: [RouterOutlet, ToastOutlet],
  selector: 'app-root',
  template: '<router-outlet /><app-toast-outlet />',
})
export class App {
  private readonly theme = inject(ThemeService);

  constructor() {
    this.theme.initialize();
  }
}

import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../core/auth/auth.service';
import { Brand } from '../shared/ui/brand';

@Component({
  selector: 'app-not-found',
  imports: [RouterLink, Brand],
  template: `<main class="not-found"><app-brand /><div><span class="not-found__code">404</span>
              <h1>This page is outside the system map.</h1><p>The address may be incorrect or the page may have moved.</p>
              <a class="button button--primary" [routerLink]="home()">Return to Solaris</a></div></main>`,
})
export class NotFound {
  private readonly auth = inject(AuthService);
  home(): string { return this.auth.isAuthenticated() ? this.auth.redirectForRole() : '/login'; }
}

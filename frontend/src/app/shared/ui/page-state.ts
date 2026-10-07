import { Component, input, output } from '@angular/core';

@Component({
  selector: 'app-page-state',
  template: `
    <section class="page-state" [attr.aria-busy]="kind() === 'loading'">
      @if (kind() === 'loading') {
        <span class="spinner" aria-hidden="true"></span>
      } @else {
        <span class="page-state__icon" aria-hidden="true">{{ kind() === 'error' ? '!' : '—' }}</span>
      }
      <h2>{{ title() }}</h2>
      <p>{{ message() }}</p>
      @if (kind() === 'error') { <button class="button button--secondary" type="button" (click)="retry.emit()">Try again</button> }
    </section>
  `,
})
export class PageState {
  readonly kind = input.required<'loading' | 'empty' | 'error'>();
  readonly title = input.required<string>();
  readonly message = input.required<string>();
  readonly retry = output<void>();
}

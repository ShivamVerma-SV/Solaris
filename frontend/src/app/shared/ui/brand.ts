import { Component, input } from '@angular/core';

@Component({
  selector: 'app-brand',
  template: `
    <div class="brand" [class.brand--compact]="compact()">
      <span class="brand__mark" aria-hidden="true"><span></span></span>
      <span class="brand__text"><strong>Solaris</strong>@if (!compact()) { <small>Energy intelligence</small> }</span>
    </div>
  `,
})
export class Brand { readonly compact = input(false); }

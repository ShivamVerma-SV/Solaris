import { Directive, ElementRef, afterNextRender, inject } from '@angular/core';

@Directive({ selector: '[appModalAutofocus]' })
export class ModalAutofocus {
  private readonly element = inject<ElementRef<HTMLElement>>(ElementRef);

  constructor() {
    afterNextRender(() => this.element.nativeElement.focus());
  }
}

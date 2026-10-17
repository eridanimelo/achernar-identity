import { Directive, ElementRef, inject } from '@angular/core';

/** Após o envio inválido, move o foco para o primeiro campo com erro (ui-map §3). */
@Directive({
  selector: 'form[appFocusInvalid]',
  host: { '(submit)': 'focusFirstInvalid()' },
})
export class FocusInvalidDirective {
  private readonly host = inject<ElementRef<HTMLFormElement>>(ElementRef);

  focusFirstInvalid(): void {
    queueMicrotask(() => {
      const el = this.host.nativeElement.querySelector<HTMLElement>(
        'input.ng-invalid, textarea.ng-invalid, mat-select.ng-invalid, [aria-invalid="true"]',
      );
      el?.focus();
    });
  }
}

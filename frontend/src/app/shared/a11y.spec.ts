import { Component, provideZonelessChangeDetection } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormControl, Validators } from '@angular/forms';
import { Translation, TranslocoLoader, provideTransloco } from '@jsverse/transloco';
import axe from 'axe-core';
import { of } from 'rxjs';
import ptBR from '../../../public/i18n/pt-BR.json';
import { EmptyStateComponent } from './components/empty-state/empty-state.component';
import { FormAlertComponent } from './components/form-alert/form-alert.component';
import { StatusChipComponent } from './components/status-chip/status-chip.component';
import { TextFieldComponent } from './components/text-field/text-field.component';

class StaticLoader implements TranslocoLoader {
  getTranslation() {
    return of(ptBR as Translation);
  }
}

@Component({
  selector: 'app-a11y-host',
  imports: [EmptyStateComponent, FormAlertComponent, StatusChipComponent, TextFieldComponent],
  template: `
    <main>
      <h1>Teste</h1>
      <app-form-alert message="Algo deu errado" traceId="trace-123" />
      <app-text-field name="email" label="E-mail" [control]="email" [required]="true" />
      <app-status-chip tone="success" label="Ativa" />
      <app-empty-state icon="inbox" title="Nada por aqui" text="Sem itens." />
    </main>
  `,
})
class HostComponent {
  readonly email = new FormControl('', { nonNullable: true, validators: [Validators.required] });
}

async function violations(fixture: ComponentFixture<unknown>) {
  const result = await axe.run(fixture.nativeElement as HTMLElement, {
    // jsdom não calcula layout/cores: contraste é verificado manualmente nos tokens de tema.
    rules: { 'color-contrast': { enabled: false }, region: { enabled: false } },
  });
  return result.violations.map((v) => `${v.id}: ${v.help} (${v.nodes.map((n) => n.target.join(' ')).join(', ')})`);
}

describe('acessibilidade (axe-core)', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideZonelessChangeDetection(),
        provideTransloco({
          config: { availableLangs: ['pt-BR'], defaultLang: 'pt-BR', reRenderOnLangChange: true },
          loader: StaticLoader,
        }),
      ],
    });
  });

  it('componentes compartilhados não têm violações WCAG detectáveis', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(await violations(fixture)).toEqual([]);
  });

  it('campo inválido tocado expõe erro associado ao input', async () => {
    const fixture = TestBed.createComponent(HostComponent);
    fixture.detectChanges();
    await fixture.whenStable();
    const input = (fixture.nativeElement as HTMLElement).querySelector('input') as HTMLInputElement;
    input.dispatchEvent(new Event('focus'));
    input.dispatchEvent(new Event('blur')); // o usuário sai do campo vazio
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const error = (fixture.nativeElement as HTMLElement).querySelector('mat-error') as HTMLElement;
    expect(error.textContent).toContain('Campo obrigatório');
    expect(input.getAttribute('aria-describedby')).toContain(error.id);
    expect(await violations(fixture)).toEqual([]);
  });
});

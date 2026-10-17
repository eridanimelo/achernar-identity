import { ApplicationInitStatus } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TranslocoService } from '@jsverse/transloco';
import { of } from 'rxjs';
import { appConfig } from './app.config';
import { SessionService } from './core/auth/session.service';
import { RuntimeConfigService } from './core/config/runtime-config';
import { LocaleService } from './core/services/locale.service';

describe('Inicialização da aplicação', () => {
  it('carrega configuração, idioma e sessão após a fronteira assíncrona sem perder o contexto de injeção', async () => {
    const calls: string[] = [];
    TestBed.configureTestingModule({ providers: [
      ...appConfig.providers,
      { provide: RuntimeConfigService, useValue: { load: async () => { await Promise.resolve(); calls.push('config'); } } },
      { provide: LocaleService, useValue: { init: () => calls.push('locale'), lang: () => 'pt-BR' } },
      { provide: TranslocoService, useValue: { load: (lang: string) => { calls.push(lang); return of({}); } } },
      { provide: SessionService, useValue: { init: async () => { calls.push('session'); } } },
    ] });
    await TestBed.inject(ApplicationInitStatus).donePromise;
    expect(calls).toEqual(['config', 'locale', 'pt-BR', 'session']);
  });
});

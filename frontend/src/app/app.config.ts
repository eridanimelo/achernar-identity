import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { ApplicationConfig, inject, isDevMode, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { MAT_DIALOG_DEFAULT_OPTIONS } from '@angular/material/dialog';
import { MAT_FORM_FIELD_DEFAULT_OPTIONS } from '@angular/material/form-field';
import { MAT_SNACK_BAR_DEFAULT_OPTIONS } from '@angular/material/snack-bar';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { TranslocoService, provideTransloco } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { routes } from './app.routes';
import { apiInterceptor } from './core/auth/api.interceptor';
import { SessionService } from './core/auth/session.service';
import { RuntimeConfigService } from './core/config/runtime-config';
import { TranslocoHttpLoader } from './core/i18n/transloco-loader';
import { LocaleService } from './core/services/locale.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([apiInterceptor])),
    provideTransloco({
      config: {
        availableLangs: ['pt-BR', 'en'],
        defaultLang: 'pt-BR',
        fallbackLang: 'pt-BR',
        reRenderOnLangChange: true,
        prodMode: !isDevMode(),
        missingHandler: { logMissingKey: false, useFallbackTranslation: true },
      },
      loader: TranslocoHttpLoader,
    }),
    { provide: MAT_FORM_FIELD_DEFAULT_OPTIONS, useValue: { appearance: 'outline', subscriptSizing: 'dynamic' } },
    { provide: MAT_DIALOG_DEFAULT_OPTIONS, useValue: { maxWidth: '100vw', autoFocus: 'first-tabbable', restoreFocus: true } },
    { provide: MAT_SNACK_BAR_DEFAULT_OPTIONS, useValue: { duration: 4000, verticalPosition: 'bottom' } },
    provideAppInitializer(async () => {
      await inject(RuntimeConfigService).load();
      const locale = inject(LocaleService);
      locale.init();
      await firstValueFrom(inject(TranslocoService).load(locale.lang()));
      await inject(SessionService).init();
    }),
  ],
};

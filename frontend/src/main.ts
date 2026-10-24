import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter } from '@angular/router';
import { provideTransloco, TranslocoLoader } from '@jsverse/transloco';
import { Injectable, inject, isDevMode } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { AppComponent } from './app/app.component';
import { routes } from './app/app.routes';
import { sessionInterceptor } from './app/core/auth/session.interceptor';
@Injectable({providedIn:'root'})
class TranslationLoader implements TranslocoLoader {
  private http=inject(HttpClient);
  getTranslation(lang:string) { return this.http.get<Record<string,string>>(`/i18n/${lang}.json`); }
}
bootstrapApplication(AppComponent, { providers: [provideRouter(routes), provideHttpClient(withInterceptors([sessionInterceptor])),
  provideTransloco({config:{availableLangs:['pt-BR'],defaultLang:'pt-BR',prodMode:!isDevMode()},loader:TranslationLoader})
]}).catch(() => {document.body.textContent='Não foi possível iniciar a aplicação. Atualize a página.';});

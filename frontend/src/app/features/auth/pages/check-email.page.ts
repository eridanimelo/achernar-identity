import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { AuthLayoutComponent } from '../../../shared/components/auth-layout/auth-layout.component';

/** SCR-05: informativa — igual para contas novas e já existentes (D-13). */
@Component({
  selector: 'app-check-email-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, TranslocoPipe, AuthLayoutComponent],
  template: `
    <app-auth-layout [title]="'auth.checkEmail.title' | transloco">
      <p>{{ 'auth.checkEmail.text' | transloco }}</p>
      <p class="muted">{{ 'auth.checkEmail.spam' | transloco }}</p>
      <div class="actions">
        <a mat-stroked-button routerLink="/resend-verification">{{ 'auth.resend' | transloco }}</a>
        <a mat-button routerLink="/login">{{ 'auth.backToLogin' | transloco }}</a>
      </div>
    </app-auth-layout>
  `,
})
export class CheckEmailPage {}

import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';
import { SessionService } from '../../../core/auth/session.service';
import { EmptyStateComponent } from '../../../shared/components/empty-state/empty-state.component';

type Kind = 'forbidden' | 'expired' | 'notfound';

/** SCR-23: /403, /404 e /session-expired. */
@Component({
  selector: 'app-error-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink, MatButtonModule, MatIconModule, TranslocoPipe, EmptyStateComponent],
  template: `
    <app-empty-state [level]="1" [icon]="icon()" [title]="('errorPage.' + kind + '.title') | transloco" [text]="('errorPage.' + kind + '.text') | transloco">
      @if (kind === 'expired' || !session.isAuthenticated()) {
        <a mat-flat-button routerLink="/login">{{ 'errorPage.signIn' | transloco }}</a>
      } @else {
        <a mat-flat-button routerLink="/account">{{ 'errorPage.back' | transloco }}</a>
      }
    </app-empty-state>
  `,
})
export class ErrorPage {
  readonly session = inject(SessionService);
  readonly kind = (inject(ActivatedRoute).snapshot.data['kind'] as Kind) ?? 'notfound';
  readonly icon = computed(() => (this.kind === 'expired' ? 'timer_off' : this.kind === 'forbidden' ? 'block' : 'travel_explore'));
}

import { ChangeDetectionStrategy, Component, computed, input, output } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { TranslocoPipe } from '@jsverse/transloco';
import { Provider, SocialLink } from '../../../core/auth/auth.models';
import { StatusChipComponent } from '../../../shared/components/status-chip/status-chip.component';
import { LocalDatePipe } from '../../../shared/pipes/local-date.pipe';

interface Row {
  provider: Provider;
  linkedAt: string | null;
  /** Desvincular removeria o último método de acesso (LAST_ACCESS_METHOD). */
  lastMethod: boolean;
}

/** SCR-17: vincular/desvincular provedores sociais. */
@Component({
  selector: 'app-social-links-section',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [MatButtonModule, TranslocoPipe, StatusChipComponent, LocalDatePipe],
  template: `
    <section class="section" aria-labelledby="sl-title">
      <h2 id="sl-title">{{ 'security.social.title' | transloco }}</h2>
      @if (rows().length === 0) {
        <p class="muted">{{ 'security.social.none' | transloco }}</p>
      }
      <ul class="rows">
        @for (r of rows(); track r.provider) {
          <li>
            <span class="name">{{ ('social.' + r.provider) | transloco }}</span>
            @if (r.linkedAt) {
              <app-status-chip tone="success" [label]="'security.social.linked' | transloco" />
              <span class="muted">{{ r.linkedAt | localDate: 'date' }}</span>
              <button mat-stroked-button type="button" [disabled]="busy() || r.lastMethod" (click)="unlink.emit(r.provider)"
                [attr.aria-describedby]="r.lastMethod ? 'last-' + r.provider : null">
                {{ 'security.social.unlink' | transloco }}
              </button>
              @if (r.lastMethod) {
                <span class="muted" [id]="'last-' + r.provider">{{ 'security.social.lastMethod' | transloco }}</span>
              }
            } @else {
              <app-status-chip tone="neutral" [label]="'security.social.notLinked' | transloco" />
              <button mat-stroked-button type="button" [disabled]="busy()" (click)="link.emit(r.provider)">
                {{ 'security.social.link' | transloco }}
              </button>
            }
          </li>
        }
      </ul>
    </section>
  `,
  styles: `
    .rows {
      list-style: none;
      padding: 0;
      margin: 0;
      display: grid;
      gap: 12px;
    }
    li {
      display: flex;
      flex-wrap: wrap;
      gap: 12px;
      align-items: center;
    }
    .name {
      min-width: 96px;
      font-weight: 600;
    }
  `,
})
export class SocialLinksSectionComponent {
  readonly links = input.required<SocialLink[]>();
  readonly available = input<Provider[]>([]);
  readonly hasPassword = input(false);
  readonly busy = input(false);
  readonly link = output<Provider>();
  readonly unlink = output<Provider>();

  readonly rows = computed<Row[]>(() => {
    const linked = new Map(this.links().map((l) => [l.provider, l.linked_at] as const));
    const providers = new Set<Provider>([...this.available(), ...linked.keys()]);
    const onlyOne = !this.hasPassword() && linked.size <= 1;
    return [...providers].sort().map((p) => ({
      provider: p,
      linkedAt: linked.get(p) ?? null,
      lastMethod: onlyOne && linked.has(p),
    }));
  });
}

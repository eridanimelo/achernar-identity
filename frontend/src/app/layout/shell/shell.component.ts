import { BreakpointObserver } from '@angular/cdk/layout';
import { ChangeDetectionStrategy, Component, DestroyRef, ElementRef, ViewChild, computed, inject, signal } from '@angular/core';
import { takeUntilDestroyed, toSignal } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { TranslocoPipe } from '@jsverse/transloco';
import { filter, map } from 'rxjs';
import { SessionService } from '../../core/auth/session.service';
import { RuntimeConfigService } from '../../core/config/runtime-config';
import { ConnectivityService } from '../../core/services/connectivity.service';
import { LoadingService } from '../../core/services/loading.service';
import { BrandComponent } from '../../shared/components/brand/brand.component';
import { ADMIN_NAV, USER_NAV } from '../navigation/nav-items';
import { PreferencesMenuComponent } from '../navigation/preferences-menu.component';

/** SCR-22/31: estrutura da aplicação — skip link, landmarks, navegação por papel e estados globais. */
@Component({
  selector: 'app-shell',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [
    RouterOutlet, RouterLink, RouterLinkActive, MatSidenavModule, MatListModule, MatIconModule, MatButtonModule,
    MatMenuModule, MatProgressBarModule, TranslocoPipe, BrandComponent, PreferencesMenuComponent,
  ],
  templateUrl: './shell.component.html',
  styleUrl: './shell.component.scss',
})
export class ShellComponent {
  private readonly router = inject(Router);
  private readonly bp = inject(BreakpointObserver);
  private readonly destroyRef = inject(DestroyRef);
  readonly session = inject(SessionService);
  readonly loading = inject(LoadingService);
  readonly conn = inject(ConnectivityService);
  readonly version = inject(RuntimeConfigService).config().version;

  @ViewChild('main', { static: true }) main!: ElementRef<HTMLElement>;
  @ViewChild('drawer') drawer?: MatSidenav;

  readonly wide = toSignal(this.bp.observe('(min-width: 960px)').pipe(map((s) => s.matches)), { initialValue: true });
  readonly userNav = USER_NAV;
  readonly adminNav = ADMIN_NAV;
  readonly showSidebar = computed(() => this.session.isAuthenticated());
  readonly email = computed(() => this.session.account()?.email ?? '');
  readonly retrying = signal(false);

  constructor() {
    this.router.events
      .pipe(filter((e): e is NavigationEnd => e instanceof NavigationEnd), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        queueMicrotask(() => this.focusHeading());
        if (!this.wide()) {
          void this.drawer?.close();
        }
      });
  }

  skipToContent(ev: Event): void {
    ev.preventDefault();
    this.main.nativeElement.focus();
  }

  async signOut(): Promise<void> {
    await this.session.logout();
  }

  async retry(): Promise<void> {
    this.retrying.set(true);
    try {
      await this.session.refresh();
      this.conn.markAvailable();
    } finally {
      this.retrying.set(false);
    }
  }

  /** Foco no h1 a cada navegação (WCAG 2.4.3) — anuncia a nova página a leitores de tela. */
  private focusHeading(): void {
    const h1 = this.main.nativeElement.querySelector<HTMLElement>('h1');
    (h1 ?? this.main.nativeElement).focus({ preventScroll: false });
  }
}

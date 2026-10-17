import { ChangeDetectionStrategy, Component, OnInit, computed, inject, signal, viewChild } from '@angular/core';
import { MatDialog } from '@angular/material/dialog';
import { TranslocoPipe, TranslocoService } from '@jsverse/transloco';
import { firstValueFrom } from 'rxjs';
import { Provider } from '../../../core/auth/auth.models';
import { SessionService } from '../../../core/auth/session.service';
import { SocialFlowService } from '../../../core/auth/social-flow.service';
import { RuntimeConfigService } from '../../../core/config/runtime-config';
import { ApiError, toApiError } from '../../../core/http/api-error';
import { ErrorMessageService } from '../../../core/services/error-message.service';
import { NotifyService } from '../../../core/services/notify.service';
import { ConfirmDialogComponent, ConfirmResult } from '../../../shared/components/confirm-dialog/confirm-dialog.component';
import { FormAlertComponent } from '../../../shared/components/form-alert/form-alert.component';
import { PageHeaderComponent } from '../../../shared/components/page-header/page-header.component';
import { AuthApiService } from '../../auth/services/auth-api.service';
import { EmailSectionComponent } from '../components/email-section.component';
import { PasswordSectionComponent, PasswordSubmit } from '../components/password-section.component';
import { SocialLinksSectionComponent } from '../components/social-links-section.component';
import { AccountService } from '../services/account.service';

/** SCR-10 / SCR-13 / SCR-17: senha, e-mail e vínculos sociais. Página inteligente. */
@Component({
  selector: 'app-security-page',
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [TranslocoPipe, PageHeaderComponent, FormAlertComponent, PasswordSectionComponent, EmailSectionComponent, SocialLinksSectionComponent],
  templateUrl: './security.page.html',
  styleUrl: './security.page.scss',
})
export class SecurityPage implements OnInit {
  private readonly api = inject(AccountService);
  private readonly auth = inject(AuthApiService);
  private readonly dialog = inject(MatDialog);
  private readonly notify = inject(NotifyService);
  private readonly errors = inject(ErrorMessageService);
  private readonly social = inject(SocialFlowService);
  private readonly cfg = inject(RuntimeConfigService);
  private readonly i18n = inject(TranslocoService);
  readonly session = inject(SessionService);

  readonly passwordSection = viewChild(PasswordSectionComponent);
  readonly account = computed(() => this.session.account());
  readonly available = signal<Provider[]>([]);
  readonly busyPassword = signal(false);
  readonly busyEmail = signal(false);
  readonly busySocial = signal(false);
  readonly emailSent = signal(false);
  readonly passwordFailure = signal<ApiError | null>(null);
  readonly emailFailure = signal<ApiError | null>(null);
  readonly alert = signal<ApiError | null>(null);
  readonly alertMessage = computed(() => {
    const a = this.alert();
    return a ? this.errors.message(a) : '';
  });

  async ngOnInit(): Promise<void> {
    try {
      this.available.set((await firstValueFrom(this.auth.config())).providers);
    } catch {
      this.available.set([]);
    }
  }

  async changePassword(v: PasswordSubmit): Promise<void> {
    this.busyPassword.set(true);
    this.passwordFailure.set(null);
    this.alert.set(null);
    try {
      await firstValueFrom(this.api.changePassword({ current_password: v.current, new_password: v.next }));
      this.passwordSection()?.reset();
      await this.session.loadAccount();
      this.notify.success('security.password.done');
    } catch (e) {
      const err = toApiError(e);
      if (err.errors.length > 0) {
        this.passwordFailure.set(err);
      } else if (err.code !== 'REAUTH_REQUIRED') {
        this.alert.set(err);
      }
    } finally {
      this.busyPassword.set(false);
    }
  }

  async changeEmail(newEmail: string): Promise<void> {
    this.busyEmail.set(true);
    this.emailFailure.set(null);
    this.emailSent.set(false);
    this.alert.set(null);
    try {
      await firstValueFrom(this.api.requestEmailChange(newEmail, this.cfg.clientId));
      this.emailSent.set(true);
    } catch (e) {
      const err = toApiError(e);
      if (err.errors.length > 0) {
        this.emailFailure.set(err);
      } else {
        this.alert.set(err);
      }
    } finally {
      this.busyEmail.set(false);
    }
  }

  async link(p: Provider): Promise<void> {
    this.busySocial.set(true);
    this.alert.set(null);
    try {
      await this.social.startIntent('link', p, '/account/security');
    } catch (e) {
      this.alert.set(toApiError(e));
      this.busySocial.set(false);
    }
  }

  async unlink(p: Provider): Promise<void> {
    const name = this.i18n.translate(`social.${p}`);
    const ref = this.dialog.open<ConfirmDialogComponent, unknown, ConfirmResult>(ConfirmDialogComponent, {
      width: '440px',
      ariaLabelledBy: 'confirm-title',
      data: {
        title: this.i18n.translate('security.social.unlinkTitle', { provider: name }),
        message: this.i18n.translate('security.social.unlinkMessage', { provider: name }),
        confirmLabel: this.i18n.translate('security.social.unlink'),
        destructive: true,
      },
    });
    const result = await firstValueFrom(ref.afterClosed());
    if (!result?.confirmed) {
      return;
    }
    this.busySocial.set(true);
    this.alert.set(null);
    try {
      await firstValueFrom(this.api.unlinkSocial(p));
      await this.session.loadAccount();
      this.notify.success('security.social.unlinked');
    } catch (e) {
      const err = toApiError(e);
      if (err.code !== 'REAUTH_REQUIRED') {
        this.alert.set(err);
      }
    } finally {
      this.busySocial.set(false);
    }
  }
}

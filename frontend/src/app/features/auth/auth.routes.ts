import { Routes } from '@angular/router';
import { guestGuard, publicGuard } from '../../core/auth/guards';

/** Rotas públicas. Páginas de credenciais exigem visitante; links de e-mail servem a qualquer um. */
export const AUTH_ROUTES: Routes = [
  { path: 'login', canActivate: [guestGuard], loadComponent: () => import('./pages/login.page').then((m) => m.LoginPage) },
  { path: 'register', canActivate: [guestGuard], loadComponent: () => import('./pages/register.page').then((m) => m.RegisterPage) },
  {
    path: 'register/check-email',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/check-email.page').then((m) => m.CheckEmailPage),
  },
  {
    path: 'resend-verification',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/email-only.page').then((m) => m.EmailOnlyPage),
    data: { kind: 'resend' },
  },
  {
    path: 'forgot-password',
    canActivate: [guestGuard],
    loadComponent: () => import('./pages/email-only.page').then((m) => m.EmailOnlyPage),
    data: { kind: 'forgot' },
  },
  { path: 'verify-email', canActivate: [publicGuard], loadComponent: () => import('./pages/verify-email.page').then((m) => m.VerifyEmailPage) },
  {
    path: 'reset-password',
    canActivate: [publicGuard],
    loadComponent: () => import('./pages/reset-password.page').then((m) => m.ResetPasswordPage),
  },
  {
    path: 'confirm-email-change',
    canActivate: [publicGuard],
    loadComponent: () => import('./pages/confirm-email-change.page').then((m) => m.ConfirmEmailChangePage),
  },
  {
    path: 'auth/callback',
    canActivate: [publicGuard],
    loadComponent: () => import('./pages/social-callback.page').then((m) => m.SocialCallbackPage),
  },
];

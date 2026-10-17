import { inject } from '@angular/core';
import { Routes } from '@angular/router';
import { SessionService } from './core/auth/session.service';
import { adminGuard, authGuard, setupGuard } from './core/auth/guards';

export const routes: Routes = [
  {
    path: '',
    pathMatch: 'full',
    redirectTo: () => (inject(SessionService).isAuthenticated() ? '/account' : '/login'),
  },
  {
    path: 'setup',
    canActivate: [setupGuard],
    loadChildren: () => import('./features/setup/setup.routes').then((m) => m.SETUP_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('./features/auth/auth.routes').then((m) => m.AUTH_ROUTES),
  },
  {
    path: 'account',
    canActivate: [authGuard],
    loadChildren: () => import('./features/account/account.routes').then((m) => m.ACCOUNT_ROUTES),
  },
  {
    path: 'admin',
    canActivate: [adminGuard],
    loadChildren: () => import('./features/admin/admin.routes').then((m) => m.ADMIN_ROUTES),
  },
  {
    path: '',
    loadChildren: () => import('./features/errors/errors.routes').then((m) => m.ERROR_ROUTES),
  },
];

import { Routes } from '@angular/router';

export const ACCOUNT_ROUTES: Routes = [
  { path: '', pathMatch: 'full', loadComponent: () => import('./pages/account.page').then((m) => m.AccountPage) },
  { path: 'security', loadComponent: () => import('./pages/security.page').then((m) => m.SecurityPage) },
  { path: 'sessions', loadComponent: () => import('./pages/sessions.page').then((m) => m.SessionsPage) },
];

import { Routes } from '@angular/router';

export const ERROR_ROUTES: Routes = [
  {
    path: '403',
    loadComponent: () => import('./pages/error.page').then((m) => m.ErrorPage),
    data: { kind: 'forbidden' },
  },
  {
    path: 'session-expired',
    loadComponent: () => import('./pages/error.page').then((m) => m.ErrorPage),
    data: { kind: 'expired' },
  },
  {
    path: '404',
    loadComponent: () => import('./pages/error.page').then((m) => m.ErrorPage),
    data: { kind: 'notfound' },
  },
  {
    path: '**',
    loadComponent: () => import('./pages/error.page').then((m) => m.ErrorPage),
    data: { kind: 'notfound' },
  },
];

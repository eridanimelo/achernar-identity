import { Routes } from '@angular/router';

export const ADMIN_ROUTES: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'accounts' },
  { path: 'accounts', loadComponent: () => import('./pages/accounts-list.page').then((m) => m.AccountsListPage) },
  { path: 'accounts/:id', loadComponent: () => import('./pages/account-detail.page').then((m) => m.AccountDetailPage) },
  { path: 'applications', loadComponent: () => import('./pages/applications-list.page').then((m) => m.ApplicationsListPage) },
  { path: 'applications/new', loadComponent: () => import('./pages/application-form.page').then((m) => m.ApplicationFormPage) },
  { path: 'applications/:id', loadComponent: () => import('./pages/application-form.page').then((m) => m.ApplicationFormPage) },
  { path: 'apis', loadComponent: () => import('./pages/apis-list.page').then((m) => m.ApisListPage) },
  { path: 'apis/new', loadComponent: () => import('./pages/api-form.page').then((m) => m.ApiFormPage) },
  { path: 'apis/:id', loadComponent: () => import('./pages/api-form.page').then((m) => m.ApiFormPage) },
  { path: 'keys', loadComponent: () => import('./pages/keys.page').then((m) => m.KeysPage) },
  { path: 'events', loadComponent: () => import('./pages/events.page').then((m) => m.EventsPage) },
];

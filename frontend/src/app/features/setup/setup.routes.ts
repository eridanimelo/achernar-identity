import { Routes } from '@angular/router';

export const SETUP_ROUTES: Routes = [
  { path: '', loadComponent: () => import('./pages/setup.page').then((m) => m.SetupPage) },
];

import { Routes } from '@angular/router';
import { authGuard, adminGuard } from './core/auth/guards';
export const routes:Routes=[
 {path:'',pathMatch:'full',redirectTo:'account'},
 {path:'auth',loadChildren:()=>import('./features/auth/auth.routes').then(m=>m.AUTH_ROUTES)},
 {path:'account',canActivate:[authGuard],loadChildren:()=>import('./features/account/account.routes').then(m=>m.ACCOUNT_ROUTES)},
 {path:'profiles',canActivate:[adminGuard],loadChildren:()=>import('./features/profiles/profiles.routes').then(m=>m.PROFILE_ROUTES)},
 {path:'admin',canActivate:[adminGuard],loadChildren:()=>import('./features/admin/admin.routes').then(m=>m.ADMIN_ROUTES)},
 {path:'**',redirectTo:'account'}
];

import { Routes } from '@angular/router';
import { AuthPage } from './pages/auth.page';
export const AUTH_ROUTES:Routes=[{path:':mode',component:AuthPage},{path:'',pathMatch:'full',redirectTo:'login'}];

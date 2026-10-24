import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../../features/auth/services/auth.service';
export const authGuard:CanActivateFn=async()=>{const auth=inject(AuthService);const router=inject(Router);return await auth.restore()?true:router.createUrlTree(['/auth/login']);};
export const adminGuard:CanActivateFn=async()=>{const auth=inject(AuthService);const router=inject(Router);const user=await auth.restore();return user?.profiles.includes('ADMIN')?true:router.createUrlTree([user?'/account':'/auth/login']);};

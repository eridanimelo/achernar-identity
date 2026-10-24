import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { SessionService } from './session.service';
export const sessionInterceptor:HttpInterceptorFn=(req,next)=>{
 const session=inject(SessionService);const router=inject(Router);
 return next(req).pipe(catchError(error=>{if(error.status===401 && !req.url.includes('/auth/login')){session.user.set(null);if(req.url!=='/api/me' && !router.url.startsWith('/auth/'))void router.navigate(['/auth/login']);}return throwError(()=>error);}));
};

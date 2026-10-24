import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CsrfService } from '../../../core/auth/csrf.service';
import { SessionService } from '../../../core/auth/session.service';
import { AuthFormData, AuthMode, IdentityUser, Message } from '../models/auth.model';
@Injectable({providedIn:'root'})
export class AuthService {
 private http=inject(HttpClient);private csrf=inject(CsrfService);readonly session=inject(SessionService);
 async restore():Promise<IdentityUser|null>{try{const user=await firstValueFrom(this.http.get<IdentityUser>('/api/me'));this.session.user.set(user);return user;}catch{this.session.user.set(null);return null;}}
 async submit(mode:AuthMode,data:AuthFormData,token:string):Promise<string>{
   const headers=await this.csrf.headers();
   if(mode==='login'){const user=await firstValueFrom(this.http.post<IdentityUser>('/api/auth/login',{email:data.email,password:data.password},{headers}));this.session.user.set(user);return '';}
   const body=mode==='register'?data:(mode==='reset-password' || mode==='activate')?{token,password:data.password}:mode==='verify'?{token}:{email:data.email};
   return (await firstValueFrom(this.http.post<Message>(`/api/auth/${mode}`,body,{headers}))).message;
 }
 async logout(){try{await firstValueFrom(this.http.post('/api/auth/logout',{}, {headers:await this.csrf.headers()}));}finally{this.session.user.set(null);}}
}

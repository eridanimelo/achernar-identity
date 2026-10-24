import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CsrfService } from '../../../core/auth/csrf.service';
import { SessionService } from '../../../core/auth/session.service';
import { IdentityUser } from '../../auth/models/auth.model';
@Injectable({providedIn:'root'})
export class AccountService {
 private http=inject(HttpClient);private csrf=inject(CsrfService);readonly session=inject(SessionService);
 async rename(name:string){const user=await firstValueFrom(this.http.patch<IdentityUser>('/api/me',{name},{headers:await this.csrf.headers()}));this.session.user.set(user);}
}

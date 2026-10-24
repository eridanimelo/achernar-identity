import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { Page } from '../../../core/models/page.model';
import { CsrfService } from '../../../core/auth/csrf.service';
import { ManagedUser, UserChange, UserDraft } from '../models/admin.model';
@Injectable({providedIn:'root'})
export class AdminService {
 private http=inject(HttpClient);private csrf=inject(CsrfService);
 list(q:string,page:number){return firstValueFrom(this.http.get<Page<ManagedUser>>('/api/admin/users',{params:new HttpParams().set('q',q).set('page',page).set('size',10)}));}
 get(id:string){return firstValueFrom(this.http.get<ManagedUser>(`/api/admin/users/${id}`));}
 async create(draft:UserDraft){return firstValueFrom(this.http.post<ManagedUser>('/api/admin/users',{name:draft.name,email:draft.email,profiles:draft.profiles},{headers:await this.csrf.headers()}));}
 async change(id:string,change:UserChange){return firstValueFrom(this.http.patch<ManagedUser>(`/api/admin/users/${id}`,change,{headers:await this.csrf.headers()}));}
 async remove(id:string){return firstValueFrom(this.http.delete(`/api/admin/users/${id}`,{headers:await this.csrf.headers()}));}
}

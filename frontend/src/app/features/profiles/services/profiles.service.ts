import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { CsrfService } from '../../../core/auth/csrf.service';
import { Page } from '../../../core/models/page.model';
import { AccessProfile, ProfileDraft, ProfileQuery } from '../models/profile.model';
@Injectable({providedIn:'root'})
export class ProfilesService {
 private http=inject(HttpClient);private csrf=inject(CsrfService);
 list(query:ProfileQuery,size=20){let params=new HttpParams().set('q',query.q).set('page',query.page).set('size',size);if(query.active!==null)params=params.set('active',query.active);return firstValueFrom(this.http.get<Page<AccessProfile>>('/api/admin/profiles',{params}));}
 get(id:string){return firstValueFrom(this.http.get<AccessProfile>(`/api/admin/profiles/${id}`));}
 async options(){const result:AccessProfile[]=[];let page=0;let totalPages=1;while(page<totalPages){const data=await this.list({q:'',active:null,page},100);result.push(...data.items);totalPages=data.totalPages;page++;}return result;}
 async save(draft:ProfileDraft){const headers=await this.csrf.headers();if(draft.id)return firstValueFrom(this.http.patch<AccessProfile>(`/api/admin/profiles/${draft.id}`,{name:draft.name,description:draft.description,active:draft.active},{headers}));return firstValueFrom(this.http.post<AccessProfile>('/api/admin/profiles',{code:draft.code,name:draft.name,description:draft.description},{headers}));}
 async remove(id:string){await firstValueFrom(this.http.delete(`/api/admin/profiles/${id}`,{headers:await this.csrf.headers()}));}
}

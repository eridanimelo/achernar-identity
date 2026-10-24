import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
@Injectable({providedIn:'root'})
export class CsrfService {
 private http=inject(HttpClient);
 async headers():Promise<HttpHeaders>{const token=await firstValueFrom(this.http.get<{token:string;headerName:string}>('/api/csrf'));return new HttpHeaders().set(token.headerName,token.token);}
}

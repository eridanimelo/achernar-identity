import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AccountService } from './account.service';
describe('AccountService',()=>{
 it('salva apenas o nome e atualiza a sessão',async()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});const service=TestBed.inject(AccountService);const http=TestBed.inject(HttpTestingController);const result=service.rename('Ana');http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-XSRF-TOKEN'});await new Promise(resolve=>setTimeout(resolve,0));const req=http.expectOne('/api/me');expect(req.request.body).toEqual({name:'Ana'});req.flush({id:'u',name:'Ana',role:'USER'});await result;expect(service.session.user()?.name).toBe('Ana');http.verify();});
});

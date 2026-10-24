import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AdminService } from './admin.service';
describe('AdminService',()=>{
 let service:AdminService;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(AdminService);http=TestBed.inject(HttpTestingController);});afterEach(()=>http.verify());
 it('envia busca e paginação vinculadas',async()=>{const result=service.list('Ana & João',2);const req=http.expectOne(r=>r.url==='/api/admin/users');expect(req.request.params.get('q')).toBe('Ana & João');expect(req.request.params.get('page')).toBe('2');req.flush({items:[],page:2,size:10,total:0,totalPages:0});expect((await result).items).toEqual([]);});
 it('protege exclusão com CSRF',async()=>{const result=service.remove('u');http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-XSRF-TOKEN'});await new Promise(resolve=>setTimeout(resolve,0));const req=http.expectOne('/api/admin/users/u');expect(req.request.method).toBe('DELETE');expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('csrf');req.flush(null);await result;});
});

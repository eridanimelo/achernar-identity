import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ProfilesService } from './profiles.service';
describe('ProfilesService',()=>{
 let service:ProfilesService;let http:HttpTestingController;
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(ProfilesService);http=TestBed.inject(HttpTestingController);});afterEach(()=>http.verify());
 it('envia busca, paginação e filtro de estado',async()=>{const pending=service.list({q:'Financeiro',active:false,page:2});const req=http.expectOne(r=>r.url==='/api/admin/profiles');expect(req.request.params.get('active')).toBe('false');expect(req.request.params.get('page')).toBe('2');req.flush({items:[],page:2,size:20,total:0,totalPages:0});await pending;});
 it('carrega todas as páginas das opções de associação',async()=>{const pending=service.options();const first=http.expectOne(r=>r.url==='/api/admin/profiles' && r.params.get('page')==='0');first.flush({items:[{code:'FIRST'}],page:0,size:100,total:101,totalPages:2});await new Promise(resolve=>setTimeout(resolve,0));const second=http.expectOne(r=>r.url==='/api/admin/profiles' && r.params.get('page')==='1');second.flush({items:[{code:'LAST'}],page:1,size:100,total:101,totalPages:2});expect((await pending).map(p=>p.code)).toEqual(['FIRST','LAST']);});
 it('não permite mudar o código do perfil ao editar',async()=>{const pending=service.save({id:'p',code:'FINANCEIRO',name:'Financeiro',description:'Novo texto',active:false});http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-XSRF-TOKEN'});await new Promise(resolve=>setTimeout(resolve,0));const req=http.expectOne('/api/admin/profiles/p');expect(req.request.method).toBe('PATCH');expect(req.request.body).toEqual({name:'Financeiro',description:'Novo texto',active:false});expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('csrf');req.flush({id:'p'});await pending;});
});

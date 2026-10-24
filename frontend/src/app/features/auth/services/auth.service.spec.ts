import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { SessionService } from '../../../core/auth/session.service';
import { IdentityUser } from '../models/auth.model';
describe('AuthService',()=>{
 let service:AuthService;let http:HttpTestingController;
 const user:IdentityUser={id:'u',name:'Ana',email:'a@example.com',role:'USER',status:'ACTIVE',verified:true,createdAt:'2026-10-24',profiles:['USER'],assignedProfiles:['USER']};
 beforeEach(()=>{TestBed.configureTestingModule({providers:[provideHttpClient(),provideHttpClientTesting()]});service=TestBed.inject(AuthService);http=TestBed.inject(HttpTestingController);});
 afterEach(()=>http.verify());
 it('restaura a sessão pelo servidor',async()=>{const result=service.restore();http.expectOne('/api/me').flush(user);expect(await result).toEqual(user);expect(TestBed.inject(SessionService).user()).toEqual(user);});
 it('remove sessão expirada',async()=>{TestBed.inject(SessionService).user.set(user);const result=service.restore();http.expectOne('/api/me').flush({}, {status:401,statusText:'Unauthorized'});expect(await result).toBeNull();expect(TestBed.inject(SessionService).user()).toBeNull();});
 it('obtém proteção CSRF antes de enviar credenciais',async()=>{const result=service.submit('login',{name:'',email:user.email,password:'secret'},'');http.expectOne('/api/csrf').flush({token:'csrf',headerName:'X-XSRF-TOKEN'});await new Promise(resolve=>setTimeout(resolve,0));const req=http.expectOne('/api/auth/login');expect(req.request.headers.get('X-XSRF-TOKEN')).toBe('csrf');req.flush(user);expect(await result).toBe('');});
});

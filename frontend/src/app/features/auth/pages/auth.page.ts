import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthFormComponent } from '../components/auth-form.component';
import { AuthService } from '../services/auth.service';
import { AuthMode, AuthFormData } from '../models/auth.model';
@Component({standalone:true,imports:[AuthFormComponent],templateUrl:'./auth.page.html',styleUrl:'./auth.page.scss'})
export class AuthPage {
 private auth=inject(AuthService);private route=inject(ActivatedRoute);private router=inject(Router);
 readonly mode=signal<AuthMode>('login');readonly busy=signal(false);readonly message=signal('');readonly error=signal('');private token='';
 constructor(){this.route.paramMap.subscribe(params=>{const value=params.get('mode') as AuthMode;
   if(!['login','register','forgot-password','reset-password','verify','resend','activate'].includes(value)){void this.router.navigate(['/auth/login']);return;}
   this.mode.set(value);this.message.set('');this.error.set('');
   if(location.hash){this.token=new URLSearchParams(location.hash.slice(1)).get('token')??'';history.replaceState(null,'',location.pathname);}
 });}
 async submit(data:AuthFormData){this.busy.set(true);this.message.set('');this.error.set('');
   try{this.message.set(await this.auth.submit(this.mode(),data,this.token));if(this.mode()==='login')await this.router.navigate(['/account']);}
   catch(e:unknown){this.error.set((e as {error?:{message?:string}}).error?.message??'Não foi possível conectar. Tente novamente.');}
   finally{this.busy.set(false);}
 }
}

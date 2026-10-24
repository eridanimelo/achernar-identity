import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { AccountService } from '../services/account.service';
import { AuthService } from '../../auth/services/auth.service';
import { AccountCardComponent } from '../components/account-card.component';
@Component({standalone:true,imports:[AccountCardComponent],templateUrl:'./account.page.html',styleUrl:'./account.page.scss'})
export class AccountPage {
 readonly account=inject(AccountService);private auth=inject(AuthService);private router=inject(Router);readonly message=signal('');readonly error=signal('');readonly busy=signal(false);
 async save(name:string){this.busy.set(true);this.message.set('');this.error.set('');try{await this.account.rename(name);this.message.set('Nome atualizado.');}catch(e:unknown){this.error.set((e as {error?:{message?:string}}).error?.message??'Não foi possível salvar.');}finally{this.busy.set(false);}}
 async logout(){try{await this.auth.logout();await this.router.navigate(['/auth/login']);}catch{this.error.set('Não foi possível encerrar a sessão no servidor. Tente novamente.');}}
}

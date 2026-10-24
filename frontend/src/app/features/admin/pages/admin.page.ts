import { Component, inject, signal } from '@angular/core';
import { AdminService } from '../services/admin.service';
import { AdminAction, ManagedUser, UserDraft } from '../models/admin.model';
import { Page } from '../../../core/models/page.model';
import { SessionService } from '../../../core/auth/session.service';
import { ProfilesService } from '../../profiles/services/profiles.service';
import { AccessProfile } from '../../profiles/models/profile.model';
import { UserListComponent } from '../components/user-list.component';
@Component({standalone:true,imports:[UserListComponent],templateUrl:'./admin.page.html',styleUrl:'./admin.page.scss'})
export class AdminPage {
 private admin=inject(AdminService);private profiles=inject(ProfilesService);readonly options=signal<AccessProfile[]>([]);readonly editing=signal<UserDraft|null>(null);readonly session=inject(SessionService);readonly data=signal<Page<ManagedUser>|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');private query='';
 constructor(){void this.load(0);}
 async load(page:number){this.busy.set(true);this.error.set('');try{this.data.set(await this.admin.list(this.query,page));}catch(e:unknown){this.error.set(this.errorMessage(e));}finally{this.busy.set(false);}}
 search(q:string){this.query=q;void this.load(0);}
 async act(action:AdminAction){this.busy.set(true);this.error.set('');this.message.set('');try{if(action.remove)await this.admin.remove(action.id);else if(action.change)await this.admin.change(action.id,action.change);this.message.set('Usuário atualizado.');await this.load(this.data()?.page??0);}catch(e:unknown){this.error.set(this.errorMessage(e));}finally{this.busy.set(false);}}
 async open(id?:string){this.busy.set(true);this.error.set('');this.message.set('');try{this.options.set(await this.profiles.options());if(id){const user=await this.admin.get(id);this.editing.set({id:user.id,name:user.name,email:user.email,profiles:[...user.assignedProfiles]});}else this.editing.set({name:'',email:'',profiles:['USER']});}catch(e:unknown){this.error.set(this.errorMessage(e));}finally{this.busy.set(false);}}
 async save(draft:UserDraft){this.busy.set(true);this.error.set('');try{if(draft.id)await this.admin.change(draft.id,{name:draft.name,email:draft.email,profiles:draft.profiles});else await this.admin.create(draft);this.editing.set(null);this.message.set(draft.id?'Usuário atualizado. Alterações de e-mail exigem nova confirmação.':'Usuário cadastrado. Convite de ativação enviado por e-mail.');await this.load(0);}catch(e:unknown){this.error.set(this.errorMessage(e));}finally{this.busy.set(false);}}
 private errorMessage(e:unknown){return (e as {error?:{message?:string}}).error?.message??'Não foi possível carregar os dados. Tente novamente.';}
}

import { Component, inject, signal } from '@angular/core';
import { ProfilesService } from '../services/profiles.service';
import { ProfileCatalogComponent } from '../components/profile-catalog.component';
import { AccessProfile, ProfileDraft, ProfileQuery } from '../models/profile.model';
import { Page } from '../../../core/models/page.model';
@Component({standalone:true,imports:[ProfileCatalogComponent],templateUrl:'./profiles.page.html',styleUrl:'./profiles.page.scss'})
export class ProfilesPage {
 private profiles=inject(ProfilesService);readonly data=signal<Page<AccessProfile>|null>(null);readonly busy=signal(false);readonly error=signal('');readonly message=signal('');readonly editing=signal<ProfileDraft|null>(null);private query:ProfileQuery={q:'',active:null,page:0};
 constructor(){void this.load();}
 async load(){this.busy.set(true);this.error.set('');try{this.data.set(await this.profiles.list(this.query));}catch(e:unknown){this.error.set(this.errorText(e));}finally{this.busy.set(false);}}
 search(query:ProfileQuery){this.query=query;void this.load();}
 async edit(id:string){this.busy.set(true);this.error.set('');try{const profile=await this.profiles.get(id);this.editing.set({...profile});}catch(e:unknown){this.error.set(this.errorText(e));}finally{this.busy.set(false);}}
 create(){this.message.set('');this.editing.set({code:'',name:'',description:'',active:true});}
 async save(draft:ProfileDraft){this.busy.set(true);this.error.set('');try{await this.profiles.save(draft);this.editing.set(null);this.message.set(draft.id?'Perfil atualizado.':'Perfil cadastrado.');await this.load();}catch(e:unknown){this.error.set(this.errorText(e));}finally{this.busy.set(false);}}
 async toggle(profile:AccessProfile){await this.save({...profile,active:!profile.active});}
 async remove(id:string){this.busy.set(true);this.error.set('');try{await this.profiles.remove(id);this.message.set('Perfil excluído.');if(this.data()?.items.length===1 && this.query.page>0)this.query.page--;await this.load();}catch(e:unknown){this.error.set(this.errorText(e));}finally{this.busy.set(false);}}
 private errorText(e:unknown){return (e as {error?:{message?:string}}).error?.message??'Não foi possível concluir. Tente novamente.';}
}

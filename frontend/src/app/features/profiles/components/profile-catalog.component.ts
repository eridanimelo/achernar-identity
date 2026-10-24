import { Component, effect, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { A11yModule } from '@angular/cdk/a11y';
import { Page } from '../../../core/models/page.model';
import { AccessProfile, ProfileDraft, ProfileQuery } from '../models/profile.model';
@Component({selector:'app-profile-catalog',standalone:true,imports:[FormsModule,MatButtonModule,A11yModule],templateUrl:'./profile-catalog.component.html',styleUrl:'./profile-catalog.component.scss'})
export class ProfileCatalogComponent {
 readonly data=input<Page<AccessProfile>|null>(null);readonly busy=input(false);readonly error=input('');readonly message=input('');readonly editing=input<ProfileDraft|null>(null);
 readonly searched=output<ProfileQuery>();readonly created=output<void>();readonly edited=output<string>();readonly saved=output<ProfileDraft>();readonly cancelled=output<void>();readonly toggled=output<AccessProfile>();readonly removed=output<string>();readonly pendingDelete=signal<AccessProfile|null>(null);
 query='';filter='all';code='';name='';description='';active=true;
 constructor(){effect(()=>{const draft=this.editing();this.code=draft?.code??'';this.name=draft?.name??'';this.description=draft?.description??'';this.active=draft?.active??true;});}
 search(page=0){this.searched.emit({q:this.query,active:this.filter==='all'?null:this.filter==='true',page});}
 submit(){this.saved.emit({id:this.editing()?.id,code:this.code,name:this.name.trim(),description:this.description.trim(),active:this.active});}
 remove(){const profile=this.pendingDelete();if(profile){this.removed.emit(profile.id);this.pendingDelete.set(null);}}
}

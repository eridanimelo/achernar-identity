import { Component, effect, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { AccessProfile } from '../../profiles/models/profile.model';
import { UserDraft } from '../models/admin.model';
@Component({selector:'app-user-editor',standalone:true,imports:[FormsModule,MatButtonModule],templateUrl:'./user-editor.component.html',styleUrl:'./user-editor.component.scss'})
export class UserEditorComponent {
 readonly user=input.required<UserDraft>();readonly profiles=input<AccessProfile[]>([]);readonly busy=input(false);readonly saved=output<UserDraft>();readonly cancelled=output<void>();name='';email='';selected:string[]=[];
 constructor(){effect(()=>{const user=this.user();this.name=user.name;this.email=user.email;this.selected=[...user.profiles];});}
 toggle(code:string,checked:boolean){this.selected=checked?[...this.selected,code]:this.selected.filter(value=>value!==code);}
 submit(){this.saved.emit({id:this.user().id,name:this.name.trim(),email:this.email.trim(),profiles:[...this.selected]});}
}

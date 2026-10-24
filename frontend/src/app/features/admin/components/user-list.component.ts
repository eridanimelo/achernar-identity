import { Component, input, output, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { A11yModule } from '@angular/cdk/a11y';
import { MatButtonModule } from '@angular/material/button';
import { AccessProfile } from '../../profiles/models/profile.model';
import { UserEditorComponent } from './user-editor.component';
import { Page } from '../../../core/models/page.model';
import { AdminAction, ManagedUser, UserDraft } from '../models/admin.model';
@Component({selector:'app-user-list',standalone:true,imports:[FormsModule,MatButtonModule,A11yModule,UserEditorComponent],templateUrl:'./user-list.component.html',styleUrl:'./user-list.component.scss'})
export class UserListComponent {
 readonly editing=input<UserDraft|null>(null);readonly profileOptions=input<AccessProfile[]>([]);readonly opened=output<string|undefined>();readonly saved=output<UserDraft>();readonly cancelled=output<void>();
 readonly data=input<Page<ManagedUser>|null>(null);readonly selfId=input('');readonly busy=input(false);readonly message=input('');readonly error=input('');readonly searched=output<string>();readonly paged=output<number>();readonly acted=output<AdminAction>();readonly pendingDelete=signal<ManagedUser|null>(null);query='';
 remove(){const user=this.pendingDelete();if(user){this.acted.emit({id:user.id,remove:true});this.pendingDelete.set(null);}}
}

import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { IdentityUser } from '../../auth/models/auth.model';
@Component({selector:'app-account-card',standalone:true,imports:[FormsModule,MatButtonModule],templateUrl:'./account-card.component.html',styleUrl:'./account-card.component.scss'})
export class AccountCardComponent {
 readonly user=input.required<IdentityUser>();readonly busy=input(false);readonly message=input('');readonly error=input('');readonly saved=output<string>();readonly signedOut=output<void>();name='';
}

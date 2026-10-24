import { Component, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { IdentityUser } from '../../auth/models/auth.model';
@Component({selector:'app-account-card',standalone:true,imports:[FormsModule,MatButtonModule,MatIconModule],templateUrl:'./account-card.component.html',styleUrl:'./account-card.component.scss'})
export class AccountCardComponent {
 readonly user=input.required<IdentityUser>();readonly busy=input(false);readonly message=input('');readonly error=input('');readonly saved=output<string>();readonly signedOut=output<void>();name='';
}

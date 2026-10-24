import { Component, computed, input, output } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { AuthFormData, AuthMode } from '../models/auth.model';
@Component({selector:'app-auth-form',standalone:true,imports:[FormsModule,RouterLink,MatButtonModule,MatIconModule],templateUrl:'./auth-form.component.html',styleUrl:'./auth-form.component.scss'})
export class AuthFormComponent {
 readonly mode=input.required<AuthMode>();readonly busy=input(false);readonly message=input('');readonly error=input('');readonly submitted=output<AuthFormData>();
 readonly heading=computed(()=>({'login':'Bem-vindo de volta.','register':'Crie sua conta.','forgot-password':'Recupere seu acesso.','reset-password':'Uma nova senha.','verify':'Confirme seu e-mail.','resend':'Novo link de confirmação.','activate':'Ative sua conta.'}[this.mode()]));
 readonly action=computed(()=>({'login':'Entrar na minha conta','register':'Criar conta','forgot-password':'Enviar instruções','reset-password':'Salvar nova senha','verify':'Confirmar meu e-mail','resend':'Reenviar confirmação','activate':'Ativar minha conta'}[this.mode()]));
 readonly iconName=computed(()=>({'login':'log-in','register':'user-plus','forgot-password':'mail','reset-password':'key','verify':'check-circle','resend':'mail','activate':'check-circle'}[this.mode()]));
 name='';email='';password='';
 submit(){this.submitted.emit({name:this.name.trim(),email:this.email.trim(),password:this.password});}
}

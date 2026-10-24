import { Component, inject } from '@angular/core';
import { RouterLink, RouterOutlet } from '@angular/router';
import { TranslocoPipe } from '@jsverse/transloco';
import { SessionService } from './core/auth/session.service';
@Component({selector:'app-root',standalone:true,imports:[RouterOutlet,RouterLink,TranslocoPipe],templateUrl:'./app.component.html',styleUrl:'./app.component.scss'})
export class AppComponent {readonly session=inject(SessionService);}

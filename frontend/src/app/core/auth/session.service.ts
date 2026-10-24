import { Injectable, signal } from '@angular/core';
import { IdentityUser } from '../../features/auth/models/auth.model';
@Injectable({providedIn:'root'})
export class SessionService {readonly user=signal<IdentityUser|null>(null);}

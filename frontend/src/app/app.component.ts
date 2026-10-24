import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { DomSanitizer } from '@angular/platform-browser';
import { MatIconModule, MatIconRegistry } from '@angular/material/icon';
import { TranslocoPipe } from '@jsverse/transloco';
import { SessionService } from './core/auth/session.service';
@Component({
  selector:'app-root', standalone:true,
  imports:[RouterOutlet,RouterLink,RouterLinkActive,TranslocoPipe,MatIconModule],
  templateUrl:'./app.component.html', styleUrl:'./app.component.scss'
})
export class AppComponent {
  readonly session=inject(SessionService);
  constructor() {
    // Somente o recurso local estático, sem URL recebida de usuário.
    inject(MatIconRegistry).addSvgIconSet(
      inject(DomSanitizer).bypassSecurityTrustResourceUrl('/assets/icons.svg')
    );
  }
}

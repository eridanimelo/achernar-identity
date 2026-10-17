import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { SessionService } from './session.service';

/** Status da instalação (GET /setup/status), consultado uma vez e reaproveitado. */
@Injectable({ providedIn: 'root' })
export class SetupStatusService {
  private readonly http = inject(HttpClient);
  private cached: boolean | null = null;

  async setupRequired(force = false): Promise<boolean> {
    if (this.cached !== null && !force) {
      return this.cached;
    }
    try {
      const s = await firstValueFrom(this.http.get<{ setup_required: boolean }>('/setup/status'));
      this.cached = s.setup_required;
    } catch {
      this.cached = false; // falha de rede não deve trancar o login
    }
    return this.cached;
  }

  markDone(): void {
    this.cached = false;
  }
}

/** Rotas autenticadas: sem sessão ⇒ /login?next=<rota>. */
export const authGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);
  return session.isAuthenticated() ? true : router.createUrlTree(['/login'], { queryParams: { next: state.url } });
};

/** Rotas públicas: com sessão ⇒ /account; setup pendente ⇒ /setup. */
export const guestGuard: CanActivateFn = async () => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (session.isAuthenticated()) {
    return router.createUrlTree(['/account']);
  }
  const setup = inject(SetupStatusService);
  return (await setup.setupRequired()) ? router.createUrlTree(['/setup']) : true;
};

/** Rotas públicas que também servem a quem está logado (verificação de e-mail, redefinição de senha). */
export const publicGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const setup = inject(SetupStatusService);
  return (await setup.setupRequired()) ? router.createUrlTree(['/setup']) : true;
};

/** /setup só existe enquanto o setup é necessário. */
export const setupGuard: CanActivateFn = async () => {
  const router = inject(Router);
  const setup = inject(SetupStatusService);
  return (await setup.setupRequired(true)) ? true : router.createUrlTree(['/login']);
};

/** Área administrativa: sem perfil ADMIN ⇒ /403. */
export const adminGuard: CanActivateFn = (_route, state) => {
  const session = inject(SessionService);
  const router = inject(Router);
  if (!session.isAuthenticated()) {
    return router.createUrlTree(['/login'], { queryParams: { next: state.url } });
  }
  return session.isAdmin() ? true : router.createUrlTree(['/403']);
};

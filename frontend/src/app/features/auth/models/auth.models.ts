import { Provider } from '../../../core/auth/auth.models';

/** GET /auth/applications/{client_id}/config */
export interface PublicConfig {
  name: string;
  type: 'BROWSER' | 'CONFIDENTIAL';
  allow_registration: boolean;
  providers: Provider[];
}

export interface TokenResponse {
  token_type: 'Bearer';
  access_token: string;
  expires_in: number;
  session_id: string;
  audiences?: string[];
  refresh_rotated?: boolean;
}

export type AuthState = 'unknown' | 'authenticated' | 'anonymous';
export type Provider = 'google' | 'facebook';

export interface SocialLink {
  provider: Provider;
  linked_at: string;
}

/** GET /api/v1/account */
export interface AccountView {
  id: string;
  email: string;
  email_verified: boolean;
  status: 'ACTIVE' | 'PENDING_VERIFICATION' | 'DISABLED';
  role: 'USER' | 'ADMIN';
  locale: string;
  has_password: boolean;
  social_links: SocialLink[];
  created_at: string;
}

export type AccountStatus = 'ACTIVE' | 'PENDING_VERIFICATION' | 'DISABLED';
export type AccountRole = 'USER' | 'ADMIN';

export interface AdminAccountSummary {
  id: string;
  email: string;
  status: AccountStatus;
  role: AccountRole;
  email_verified: boolean;
  has_password: boolean;
  providers: string[];
  created_at: string;
}

export interface EventBrief {
  id: string;
  occurred_at: string;
  event_type: string;
  severity: string;
  outcome: string;
}

export interface AdminAccountDetail extends AdminAccountSummary {
  locale: string;
  disabled_at?: string;
  disabled_reason?: string;
  anonymized_at?: string;
  active_sessions: number;
  social_links: { provider: string; linked_at: string }[];
  recent_events: EventBrief[];
  version: number;
}

export interface AccountFilters {
  q?: string;
  status?: string;
  role?: string;
  method?: string;
  page: number;
  size: number;
  sort?: string;
}

export type RegistryStatus = 'ACTIVE' | 'DISABLED';
export type ClientType = 'BROWSER' | 'CONFIDENTIAL';

export interface ApplicationSummary {
  id: string;
  client_id: string;
  name: string;
  type: ClientType;
  status: RegistryStatus;
  allow_registration: boolean;
  identity_api_access: boolean;
  created_at: string;
}

export interface AudienceEntry {
  audience: string;
  is_default: boolean;
  active?: boolean;
}

export interface SecretRow {
  id: string;
  created_at: string;
  last_used_at?: string;
  revoked_at?: string;
}

export interface ApplicationDetail {
  id: string;
  client_id: string;
  name: string;
  description?: string;
  type: ClientType;
  status: RegistryStatus;
  allow_registration: boolean;
  identity_api_access: boolean;
  google_enabled: boolean;
  facebook_enabled: boolean;
  access_token_ttl_seconds?: number;
  origins: string[];
  redirect_uris: string[];
  verify_email_url?: string;
  reset_password_url?: string;
  confirm_email_change_url?: string;
  audiences: AudienceEntry[];
  is_system: boolean;
  created_at: string;
  updated_at: string;
  version: number;
  secrets?: SecretRow[];
  /** Presente apenas na resposta de criação de aplicação CONFIDENTIAL. */
  client_secret?: string;
}

export interface ApplicationPayload {
  client_id?: string;
  name: string;
  description: string | null;
  type?: ClientType;
  status?: RegistryStatus;
  origins: string[];
  redirect_uris: string[];
  verify_email_url: string | null;
  reset_password_url: string | null;
  confirm_email_change_url: string | null;
  allow_registration: boolean;
  identity_api_access: boolean;
  google_enabled: boolean;
  facebook_enabled: boolean;
  access_token_ttl_seconds: number | null;
  audiences: { audience: string; is_default: boolean }[];
  version?: number;
}

export interface ApiSummary {
  id: string;
  audience: string;
  name: string;
  description?: string;
  status: RegistryStatus;
  created_at: string;
  updated_at: string;
  version: number;
  credentials?: SecretRow[];
}

export interface SigningKey {
  kid: string;
  status: 'PUBLISHED' | 'ACTIVE' | 'RETIRED' | 'REMOVED';
  created_at: string;
  publish_at?: string;
  activate_at?: string;
  retire_at?: string;
  remove_at?: string;
  removal_reason?: string;
}

export interface SecurityEventItem {
  id: string;
  occurred_at: string;
  event_type: string;
  severity: string;
  outcome: string;
  account?: { id: string; email?: string };
  actor_account_id?: string;
  client_id?: string;
  session_id?: string;
  ip?: string;
  user_agent?: string;
  request_id?: string;
  details?: Record<string, unknown>;
}

export interface EventFilters {
  from?: string;
  to?: string;
  type?: string[];
  account?: string;
  client_id?: string;
  outcome?: string;
  severity?: string;
  limit?: number;
  cursor?: string;
}

/** Tipos de evento de segurança (event-catalog.md), usados no filtro de SCR-21. */
export const EVENT_TYPES: readonly string[] = [
  'ACCOUNT_REGISTERED',
  'REGISTRATION_DUPLICATE_ATTEMPT',
  'EMAIL_VERIFIED',
  'EMAIL_VERIFICATION_RESENT',
  'LOGIN_SUCCEEDED',
  'LOGIN_FAILED',
  'LOGIN_BLOCKED_UNVERIFIED',
  'LOGIN_BLOCKED_DISABLED',
  'ACCOUNT_LOCKED_THROTTLE',
  'SOCIAL_LOGIN_SUCCEEDED',
  'SOCIAL_LOGIN_FAILED',
  'SOCIAL_ACCOUNT_CREATED',
  'SOCIAL_LINK_CONFLICT',
  'PENDING_ACCOUNT_SUPERSEDED',
  'SOCIAL_LINKED',
  'SOCIAL_UNLINKED',
  'TOKEN_ISSUED',
  'REFRESH_REUSE_TOLERATED',
  'REFRESH_REUSE_DETECTED',
  'LOGOUT',
  'SESSION_REVOKED',
  'ALL_SESSIONS_REVOKED',
  'PASSWORD_RESET_REQUESTED',
  'PASSWORD_RESET_COMPLETED',
  'PASSWORD_CHANGED',
  'PASSWORD_SET',
  'REAUTH_SUCCEEDED',
  'REAUTH_FAILED',
  'EMAIL_CHANGE_REQUESTED',
  'EMAIL_CHANGED',
  'ACCOUNT_DISABLED',
  'ACCOUNT_ENABLED',
  'ADMIN_GRANTED',
  'ADMIN_REVOKED',
  'ACCOUNT_ANONYMIZED',
  'BOOTSTRAP_ADMIN_CREATED',
  'SETUP_ATTEMPT_FAILED',
  'APPLICATION_CREATED',
  'APPLICATION_UPDATED',
  'APPLICATION_DISABLED',
  'APPLICATION_ENABLED',
  'APPLICATION_SECRET_ROTATED',
  'API_CREATED',
  'API_UPDATED',
  'API_DISABLED',
  'API_ENABLED',
  'API_CREDENTIAL_ROTATED',
  'INTROSPECTION_AUTH_FAILED',
  'SIGNING_KEY_ROTATED',
  'SIGNING_KEY_REMOVED',
  'RATE_LIMIT_EXCEEDED',
  'EMAIL_DELIVERY_FAILED',
  'RETENTION_PURGE_RUN',
];

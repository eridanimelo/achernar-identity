export interface NavItem {
  path: string;
  labelKey: string;
  icon: string;
}

export const USER_NAV: NavItem[] = [
  { path: '/account', labelKey: 'nav.account', icon: 'person' },
  { path: '/account/security', labelKey: 'nav.security', icon: 'shield' },
  { path: '/account/sessions', labelKey: 'nav.sessions', icon: 'devices' },
];

export const ADMIN_NAV: NavItem[] = [
  { path: '/admin/accounts', labelKey: 'nav.accounts', icon: 'group' },
  { path: '/admin/applications', labelKey: 'nav.applications', icon: 'apps' },
  { path: '/admin/apis', labelKey: 'nav.apis', icon: 'api' },
  { path: '/admin/keys', labelKey: 'nav.keys', icon: 'key' },
  { path: '/admin/events', labelKey: 'nav.events', icon: 'history' },
];

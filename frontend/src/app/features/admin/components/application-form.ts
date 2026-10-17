import { FormArray, FormBuilder, FormControl, FormGroup, ValidatorFn, Validators } from '@angular/forms';
import { CLIENT_ID_PATTERN, isValidAbsoluteUrl } from '../../../core/utils/validators';
import { ApplicationDetail, ApplicationPayload, ClientType, RegistryStatus } from '../models/admin.models';

export type ApplicationFormGroup = FormGroup<{
  client_id: FormControl<string>;
  name: FormControl<string>;
  description: FormControl<string>;
  type: FormControl<ClientType>;
  status: FormControl<RegistryStatus>;
  allow_registration: FormControl<boolean>;
  identity_api_access: FormControl<boolean>;
  google_enabled: FormControl<boolean>;
  facebook_enabled: FormControl<boolean>;
  access_token_ttl_seconds: FormControl<string>;
  verify_email_url: FormControl<string>;
  reset_password_url: FormControl<string>;
  confirm_email_change_url: FormControl<string>;
  origins: FormControl<string[]>;
  redirect_uris: FormControl<string[]>;
  audiences: FormArray<FormGroup<{ audience: FormControl<string>; is_default: FormControl<boolean> }>>;
}>;

const urlOrEmpty: ValidatorFn = (c) => {
  const v = typeof c.value === 'string' ? c.value.trim() : '';
  return !v || (isValidAbsoluteUrl(v) && v.length <= 500) ? null : { url: true };
};

const ttlValidator: ValidatorFn = (c) => {
  const v = typeof c.value === 'string' ? c.value.trim() : '';
  if (!v) {
    return null;
  }
  const n = Number(v);
  return Number.isInteger(n) && n >= 300 && n <= 900 ? null : { range: true };
};

export function buildApplicationForm(fb: FormBuilder): ApplicationFormGroup {
  return fb.nonNullable.group({
    client_id: ['', [Validators.required, Validators.pattern(CLIENT_ID_PATTERN)]],
    name: ['', [Validators.required, Validators.maxLength(100)]],
    description: ['', [Validators.maxLength(500)]],
    type: ['BROWSER' as ClientType, [Validators.required]],
    status: ['ACTIVE' as RegistryStatus],
    allow_registration: [false],
    identity_api_access: [false],
    google_enabled: [false],
    facebook_enabled: [false],
    access_token_ttl_seconds: ['', [ttlValidator]],
    verify_email_url: ['', [urlOrEmpty]],
    reset_password_url: ['', [urlOrEmpty]],
    confirm_email_change_url: ['', [urlOrEmpty]],
    origins: [[] as string[]],
    redirect_uris: [[] as string[]],
    audiences: fb.nonNullable.array([] as FormGroup<{ audience: FormControl<string>; is_default: FormControl<boolean> }>[]),
  });
}

export function addAudience(fb: FormBuilder, form: ApplicationFormGroup, audience: string, isDefault: boolean): void {
  if (form.controls.audiences.controls.some((g) => g.controls.audience.value === audience)) {
    return;
  }
  form.controls.audiences.push(fb.nonNullable.group({ audience: [audience], is_default: [isDefault] }));
}

export function patchApplicationForm(fb: FormBuilder, form: ApplicationFormGroup, a: ApplicationDetail): void {
  form.patchValue({
    client_id: a.client_id,
    name: a.name,
    description: a.description ?? '',
    type: a.type,
    status: a.status,
    allow_registration: a.allow_registration,
    identity_api_access: a.identity_api_access,
    google_enabled: a.google_enabled,
    facebook_enabled: a.facebook_enabled,
    access_token_ttl_seconds: a.access_token_ttl_seconds ? String(a.access_token_ttl_seconds) : '',
    verify_email_url: a.verify_email_url ?? '',
    reset_password_url: a.reset_password_url ?? '',
    confirm_email_change_url: a.confirm_email_change_url ?? '',
    origins: a.origins,
    redirect_uris: a.redirect_uris,
  });
  form.controls.audiences.clear();
  for (const g of a.audiences) {
    addAudience(fb, form, g.audience, g.is_default);
  }
}

const blankToNull = (s: string): string | null => (s.trim() ? s.trim() : null);

export function toPayload(form: ApplicationFormGroup, isNew: boolean): ApplicationPayload {
  const v = form.getRawValue();
  const ttl = v.access_token_ttl_seconds.trim();
  const payload: ApplicationPayload = {
    name: v.name.trim(),
    description: blankToNull(v.description),
    origins: v.origins,
    redirect_uris: v.redirect_uris,
    verify_email_url: blankToNull(v.verify_email_url),
    reset_password_url: blankToNull(v.reset_password_url),
    confirm_email_change_url: blankToNull(v.confirm_email_change_url),
    allow_registration: v.allow_registration,
    identity_api_access: v.identity_api_access,
    google_enabled: v.google_enabled,
    facebook_enabled: v.facebook_enabled,
    access_token_ttl_seconds: ttl ? Number(ttl) : null,
    audiences: v.audiences.map((g) => ({ audience: g.audience, is_default: g.is_default })),
  };
  if (isNew) {
    payload.client_id = v.client_id.trim();
    payload.type = v.type;
  } else {
    payload.status = v.status;
  }
  return payload;
}

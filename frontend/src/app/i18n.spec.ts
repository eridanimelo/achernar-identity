import en from '../../public/i18n/en.json';
import ptBR from '../../public/i18n/pt-BR.json';

const placeholders = (s: string) => [...s.matchAll(/{{\s*(\w+)\s*}}/g)].map((m) => m[1]).sort();

describe('i18n', () => {
  const pt = ptBR as Record<string, string>;
  const english = en as Record<string, string>;

  it('pt-BR e en têm exatamente as mesmas chaves', () => {
    expect(Object.keys(english).sort()).toEqual(Object.keys(pt).sort());
  });

  it('nenhuma tradução está vazia e os parâmetros {{x}} coincidem entre idiomas', () => {
    for (const key of Object.keys(pt)) {
      expect(pt[key].trim(), key).not.toBe('');
      expect(english[key].trim(), key).not.toBe('');
      expect(placeholders(english[key]), key).toEqual(placeholders(pt[key]));
    }
  });

  it('todo código de erro conhecido da API tem mensagem', () => {
    const codes = [
      'VALIDATION_ERROR', 'PASSWORD_POLICY_VIOLATION', 'INVALID_CREDENTIALS', 'INVALID_CLIENT', 'EMAIL_NOT_VERIFIED',
      'ACCOUNT_DISABLED', 'REAUTH_REQUIRED', 'REAUTH_FAILED', 'FORBIDDEN', 'NOT_FOUND', 'CONFLICT', 'LAST_ADMIN_PROTECTED',
      'LAST_ACCESS_METHOD', 'RATE_LIMITED', 'SERVICE_UNAVAILABLE', 'INTERNAL_ERROR', 'NETWORK_ERROR', 'UNKNOWN',
    ];
    for (const c of codes) {
      expect(pt[`error.${c}`], c).toBeTruthy();
    }
  });
});

import { safeNext } from './safe-redirect';

describe('safeNext', () => {
  it('aceita caminhos internos', () => {
    expect(safeNext('/account/security')).toBe('/account/security');
    expect(safeNext('/admin/events?type=LOGIN_FAILED')).toBe('/admin/events?type=LOGIN_FAILED');
  });

  it.each([
    'https://evil.example', '//evil.example', '/\\evil.example', 'javascript:alert(1)', 'account', '', '/a\nb', '/a\u0000b',
  ])('rejeita %j', (value) => {
    expect(safeNext(value)).toBe('/account');
  });

  it('usa o fallback informado para nulos', () => {
    expect(safeNext(null, '/x')).toBe('/x');
    expect(safeNext(undefined)).toBe('/account');
  });
});

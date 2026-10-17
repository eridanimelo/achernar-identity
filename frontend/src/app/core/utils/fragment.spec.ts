import { takeFragmentToken } from './fragment';

function fakeWindow(hash: string) {
  const replaceState = vi.fn();
  const win = {
    location: { hash, pathname: '/verify-email', search: '?x=1' },
    history: { state: null, replaceState },
  } as unknown as Window;
  return { win, replaceState };
}

describe('takeFragmentToken', () => {
  it('lê o token e limpa o fragmento da barra de endereço', () => {
    const { win, replaceState } = fakeWindow('#token=abcdefghijklmnop1234');
    expect(takeFragmentToken(win)).toBe('abcdefghijklmnop1234');
    expect(replaceState).toHaveBeenCalledWith(null, '', '/verify-email?x=1');
  });

  it('rejeita token com formato inválido, mas ainda limpa o fragmento', () => {
    const { win, replaceState } = fakeWindow('#token=curto');
    expect(takeFragmentToken(win)).toBeNull();
    expect(replaceState).toHaveBeenCalled();
  });

  it('sem fragmento não faz nada', () => {
    const { win, replaceState } = fakeWindow('');
    expect(takeFragmentToken(win)).toBeNull();
    expect(replaceState).not.toHaveBeenCalled();
  });
});

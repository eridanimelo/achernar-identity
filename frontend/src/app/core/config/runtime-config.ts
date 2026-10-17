import { Injectable, signal } from '@angular/core';

export interface RuntimeConfig {
  /** Base absoluta da API do Identity ('' = mesma origem, via proxy reverso). */
  apiBaseUrl: string;
  /** client_id desta UI (aplicação BROWSER registrada). */
  clientId: string;
  version: string;
}

const DEFAULTS: RuntimeConfig = { apiBaseUrl: '', clientId: 'achernar-identity-ui', version: 'dev' };

/** Configuração em tempo de execução (public/config.json), carregada antes da inicialização do app. */
@Injectable({ providedIn: 'root' })
export class RuntimeConfigService {
  private readonly state = signal<RuntimeConfig>(DEFAULTS);
  readonly config = this.state.asReadonly();

  async load(fetcher: typeof fetch = fetch): Promise<void> {
    try {
      const res = await fetcher('config.json', { cache: 'no-store' });
      if (res.ok) {
        const raw = (await res.json()) as Partial<RuntimeConfig>;
        this.state.set({
          apiBaseUrl: stripSlash(raw.apiBaseUrl ?? DEFAULTS.apiBaseUrl),
          clientId: raw.clientId || DEFAULTS.clientId,
          version: raw.version || DEFAULTS.version,
        });
      }
    } catch {
      // mantém os padrões (mesma origem)
    }
  }

  get clientId(): string {
    return this.state().clientId;
  }

  /** Resolve um caminho da API ('/auth/login') contra a base configurada. */
  url(path: string): string {
    return /^https?:\/\//.test(path) ? path : this.state().apiBaseUrl + path;
  }

  /** Origem desta UI (usada em redirect_uri). */
  get uiOrigin(): string {
    return window.location.origin;
  }
}

function stripSlash(s: string): string {
  return s.replace(/\/+$/, '');
}

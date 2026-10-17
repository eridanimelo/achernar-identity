# Validação frontend e integração local — 2026-10-17

Intenção FRONTEND, unidade limitada à inicialização Angular e smoke integrado. Origem das features FT-01/FT-03: NEW. Implementação autorizada pela solicitação humana de continuar o fluxo e executar o projeto.

## Correções e análise
- `frontend/src/app/app.config.ts`: resolve todas as dependências com `inject()` antes do primeiro `await`; preserva a ordem configuração → idioma → tradução → sessão. Corrige NG0203 que impedia qualquer tela.
- `frontend/src/app/app.config.spec.ts`: teste de regressão verifica a conclusão assíncrona e a ordem completa através do inicializador real.
- `frontend/proxy.conf.json`: somente `/setup/status` e `/setup/admin` vão à API; `/setup` permanece navegação Angular, inclusive em atualização direta. Antes retornava JSON 404.
- Reuse Scan executado pelo contrato do especialista frontend na própria unidade (sem agente adicional): EXTEND do inicializador existente e REUSE dos serviços RuntimeConfigService, LocaleService, TranslocoService e SessionService. Índice regenerado em `.cache/reuse-index.md`; candidatos reais e consumidores analisados. Nenhum componente, model, serviço ou abstração de produto criado.
- Arquitetura: alteração transversal em configuração, sem mudar separação de features, templates, models, lazy loading ou acesso HTTP por componentes. ESLint existente aprovado.

## Evidência executada
- Node 24.19.0; `npm run verify`: i18n, ESLint, 15 arquivos Vitest e 63 testes aprovados; build de produção concluído. Log local: `/tmp/achernar-frontend-verify.log`.
- Navegador real: tela de setup renderizada com campos mascarados; fixture via POST `/setup/admin` retorna 201; nenhuma sessão emitida pelo setup. Credenciais aleatórias locais em `/tmp/achernar-local-admin.json` (0600), segredo de implantação fora do repositório.
- Após criação, abertura direta `/setup` redireciona a `/login`; login normal real abre `/account` com perfil Administrator; link Accounts abre `/admin/accounts` e exibe uma conta ativa. Atualizar a lista restaura sessão e dados. Console final sem erros.
- Screenshot: `/tmp/achernar-admin-validation.png`.
- API/backend já verificados em Java 25: 96 unitários/arquitetura + 19 IT aprovados (evidência complementar em `backend-validation.md`).

## Gates e limites
GATE-CTX/AUT/ANL: PASS para esta correção delimitada. GATE-QLT: PASS para inicialização, proxy e verificações executadas. Não equivale a cobertura integral de todas as Specs ou aprovação global de segurança/encerramento. A UI do envio de setup não foi submetida: fixture local pela API evitou cadastrar nova credencial pela automação de navegador. Não foram executados todos cenários adversariais, todas telas, mobile ou fluxos sociais reais. Google/Facebook dependem de credenciais externas não fornecidas. REVIEW e encerramento global permanecem pendentes; nenhum push realizado.

Serviços mantidos em execução: frontend localhost:4200 (sessão 38443), API localhost:8080 e readiness localhost:9090; PostgreSQL localhost:55432 e Mailpit localhost:18025. Logs em `/tmp/achernar-frontend.log` e `/tmp/achernar-backend.log`.

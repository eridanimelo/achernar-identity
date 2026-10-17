# Achernar Identity

Serviço de **autenticação reutilizável, instalado por projeto**, para aplicações próprias:
cadastro e login (e-mail/senha, Google e Facebook), tokens JWT validáveis por JWKS,
refresh tokens rotativos, sessões, recuperação de senha, troca de e-mail,
administração de contas, registro de aplicações e APIs consumidoras, eventos de
segurança, e-mails transacionais e interface própria (Angular).

Organização: Eridani · Versão do projeto: 1.0.0

> **Estado atual:** fase **documental concluída**. Discovery e as 14 Specs
> (com documentos transversais) estão escritos e verificados por script de
> rastreabilidade. **Nenhum código do produto foi implementado**, nenhuma
> configuração Docker foi alterada. A **implementação** exige revisão humana e
> intenção explícita (GATE-AUT).

Fora do escopo: SSO entre aplicações, alternativa ao Keycloak/OIDC completo,
multi-tenancy, federação de identidades, Helm/Kubernetes.

---

## 1. Visão técnica (alvo)

Fonte de decisões: `requisitos/specs/_shared/decisions.md` (D-01…D-51). Resumo:

| Camada | Alvo |
|---|---|
| Backend | Java 25 LTS · Spring Boot 4.1.x · Spring Security 7 · monólito modular · Liquibase |
| Frontend | Angular 22 (standalone) · Angular Material 3 · i18n pt-BR/en |
| Banco | PostgreSQL 18 (estado, limites, eventos e fila de e-mail no próprio banco; sem Redis) |
| Tokens | JWT RS256 + JWKS; refresh opaco rotativo; cookie HttpOnly para navegadores |
| Execução | Docker Compose (dev/prod), proxy com TLS; e-mail local por servidor de captura |

As versões são **alvo de planejamento** e serão revalidadas na primeira etapa de
implementação. Divergências entre este alvo e `.ia/config/*.yaml` estão registradas
(DEV-01…07 em `decisions.md` e `config-changes.md`); os arquivos de `.ia/config`
ainda **não** foram atualizados.

## 2. Estrutura do repositório

```text
achernar-identity/
├── README.md
├── .ia/             ← motor do framework de trabalho (abrir a sessão do agente escopada aqui)
├── requisitos/
│   ├── discovery/   ← sistema, módulos, casos de uso (47), dependências, priorização/Waves
│   ├── specs/
│   │   ├── _shared/ ← decisões, parâmetros, regras, catálogos, modelo de dados, segurança,
│   │   │              tokens, login social, e-mails, telas, operação, testes, plano, rastreabilidade
│   │   └── FT-01 … FT-14/ ← 8 arquivos por feature (business, api, database, frontend,
│   │                         security, tests, status.yaml, context-manifest.yaml)
│   ├── reviews/     ← completeness-check.md
│   ├── state/       ← current.yaml (retomada de contexto)
│   └── tools/       ← verify_specs.py (ferramenta de documentação: verifica IDs e gera a matriz)
├── docker/          ← docker-compose.yml + .env.example (inalterados nesta fase)
├── backend/         ← (futuro) ainda não criado
└── frontend/        ← (futuro) ainda não criado
```

Ponto de entrada da documentação: `requisitos/specs/_shared/index.md`.

## 3. Ordem de implementação proposta (quando autorizada)

Detalhe em `requisitos/specs/_shared/implementation-plan.md`:
E0 ambiente → E1 fundação do backend → E2 chaves/JWT e e-mail → E3 shell Angular (em paralelo) →
E4 registro de aplicações/APIs e setup → E5 cadastro e login → E6 senhas, e-mail, sessões →
E7 controle de tokens e administração → E8 login social → E9 fechamento (retenção, hardening, e2e).

## 4. Como trabalhar neste repositório

1. Abra a sessão do agente **escopada em `.ia/`**.
2. O orquestrador classifica a solicitação em uma intenção e monta o contexto mínimo.
3. Implementação de qualquer Spec exige **intenção explícita + GATE-AUT**; este README não é autorização.
4. Regras de Git em `.ia/config/git.yaml`: **nunca `git push`**; trabalhar em branch própria.
5. Para validar a documentação: `python3 requisitos/tools/verify_specs.py` (regenera `traceability.md`).

## 5. Pendências do operador (antes de produção)

Provedor SMTP, domínios/TLS, apps OAuth Google/Facebook, logotipo, geração da
chave-mestra e do segredo de bootstrap e janela de manutenção (A-02…A-07 em
`requisitos/discovery/system.md`). Nenhuma credencial real deve entrar no repositório.

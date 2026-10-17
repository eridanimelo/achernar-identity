# Validação do backend — 17/10/2026

`mvn -B verify`: **BUILD SUCCESS**, 96 testes unitários/arquitetura e 19 testes de integração, zero falhas/erros/ignorados. PostgreSQL 18 real via Testcontainers e SMTP GreenMail.

Correções somente nos testes existentes: setup encerrado retorna 404 SETUP_NOT_AVAILABLE (FT-01); bloqueio de login mantém 401 INVALID_CREDENTIALS sem Retry-After e bloqueio persistido (FT-03); auto-desativação retorna 403 FORBIDDEN e rebaixamento do último administrador retorna 409 LAST_ADMIN_PROTECTED (FT-11). Contadores de taxa são limpos entre cenários no banco descartável para evitar interferência; proteção permanece ativa durante cada teste. Não houve criação de abstração nem decisão de reuso: EXTEND dos testes conhecidos, GATE-ANL aplicável satisfeito pela comparação dos contratos e implementações.

Execução inicial com JDK 27 e `release=25` do Maven. Revalidação na versão alvo: imagem oficial `maven:3-eclipse-temurin-25`, `mvn -B clean verify`, **BUILD SUCCESS** em 24,459 s, 96 testes unitários/arquitetura e 19 IT, zero falhas/erros/ignorados. Fonte montada somente para leitura e copiada para diretório temporário do contêiner; API local preservada. Log temporário: `/tmp/achernar-java25-verify.log`.

API local iniciada com perfil dev, PostgreSQL em 55432, SMTP em 11025 e Mailpit em 18025. Stack anterior preservado. Readiness UP e setup disponível. Configuração transitória em /tmp/achernar-runtime-override.yml e /tmp/achernar-run-backend.py; bootstrap aleatório em /tmp/achernar-bootstrap-token (0600). Modo dev usa chave-mestra insegura explicitamente habilitada; não é configuração de produção.

Frontend compilou pelo servidor de desenvolvimento, mas navegador mostrou tela vazia: NG0203, LocaleService injetado após await em app.config.ts. Próxima unidade FRONTEND em contexto isolado.

GATE-QLT desta unidade: PASS para build e suíte backend existente. Não comprova todos os cenários das treze Specs, segurança completa, E2E social real ou encerramento global. GATE-CLS global pendente.

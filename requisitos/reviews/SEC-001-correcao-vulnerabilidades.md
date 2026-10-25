# Correção de achados de segurança — SCA, SAST e DAST
Data: 24/10/2026; intenção SECURITY; origem NEW (correção sobre implementação existente); resultado PASS com riscos aceitos registrados.

## Solicitação e escopo
Corrigir os 34 achados do plano de correção (21 de SCA, 2 de SAST em testes, 11 de DAST/configuração de borda). Escopo restrito a dependências do backend, configuração do Nginx do frontend e verificação; nenhuma regra de negócio, contrato de API, schema ou tela foi alterado.

## Causa e decisão
Os achados de SCA vinham de versões herdadas do BOM do Spring Boot 4.1.0 (Tomcat, Spring Framework, Jackson, Log4j, PostgreSQL JDBC) e de uma versão fixa de BouncyCastle no `pom.xml`. A correção preferida foi subir o parent para o patch atual da mesma linha (4.1.2), que já gerencia versões corrigidas, em vez de sobrescrever cada artefato isoladamente — menos desvio do BOM e menor risco de incompatibilidade. BouncyCastle não é gerenciado pelo BOM e foi atualizado explicitamente. As versões-alvo foram confirmadas no OSV (faixa corrigida de cada GHSA), não apenas no texto do plano.

## Alterações
- `backend/pom.xml`: `spring-boot-starter-parent` 4.1.0 → 4.1.2; `bcprov-jdk18on` 1.83 → 1.85.
- `frontend/nginx.conf`: Host do upstream fixado em `$proxy_host`; `Cache-Control: no-cache` no documento HTML; cabeçalhos de segurança movidos para a entrega da SPA (respostas de `/api/` e `/.well-known/` mantêm apenas os cabeçalhos do backend).
- `frontend/security-headers.conf` (novo): COOP `same-origin`, COEP `require-corp`, CORP `same-origin`, Permissions-Policy restritiva e CSP ampliada (`object-src 'none'`, `frame-src 'none'`, `worker-src 'self'`, `manifest-src 'self'`).
- `frontend/Dockerfile`: copia o snippet de cabeçalhos para `/etc/nginx/snippets/`.

## Disposição dos achados

### SCA — corrigidos (F-0001 a F-0021)
Versões efetivas após a mudança, confirmadas por `mvn dependency:tree`:

| Componente | Antes | Depois | Menor versão corrigida (OSV) |
|---|---|---|---|
| tomcat-embed-core | 11.0.22 | 11.0.27 | 11.0.25 |
| spring-webmvc | 7.0.8 | 7.0.10 | 7.0.9 |
| jackson-core / jackson-databind | 3.1.4 | 3.1.7 | 3.1.5 – 3.1.7 conforme o GHSA |
| postgresql (JDBC) | 42.7.11 | 42.7.14 | 42.7.12 |
| log4j-api | 2.25.4 | 2.25.5 | 2.25.5 |
| bcprov-jdk18on | 1.83 | 1.85 | 1.84 – 1.85 conforme o GHSA |

O maior requisito entre os GHSAs é atendido em todos os casos (BouncyCastle exigia 1.85; Jackson, 3.1.7; Tomcat, 11.0.25; Spring, 7.0.9). Não foi necessária nenhuma sobrescrita individual de versão gerenciada.

### Configuração de borda — corrigidos
- F-0025: `proxy_set_header Host $proxy_host` — o backend deixa de receber o Host do cliente.
- F-0026, F-0027, F-0028: COEP `require-corp`, COOP `same-origin` e CORP `same-origin` no documento.
- F-0032: `Permissions-Policy` desabilitando acelerômetro, câmera, geolocalização, giroscópio, magnetômetro, microfone, pagamento e USB.
- F-0034: `Cache-Control: no-cache` (com `Expires` no passado) no HTML; os bundles com hash continuam revalidáveis por ETag, sem conteúdo sensível.

### Riscos aceitos e falsos positivos
- F-0024 — CSP `style-src 'unsafe-inline'`: necessário para os estilos de componente que o Angular injeta em tempo de execução (verificadas 8 folhas `<style>` em runtime). Em build estático não há como emitir nonce por resposta; usar hashes fixos quebraria estilos a cada componente. Mitigação mantida: `script-src 'self'` sem `unsafe-inline`/`unsafe-eval`, `object-src 'none'` e `base-uri`/`form-action` restritos. Risco residual limitado a injeção de estilo, não de script.
- F-0029 — `bypassSecurityTrustResourceUrl('/assets/icons.svg')`: valor constante do build, mesma origem, sem entrada de usuário. Necessário para o `MatIconRegistry`; substituir por sanitização literal descartaria atributos SVG do sprite. Mantido e documentado no código.
- F-0022, F-0023 — senha literal em `IdentityIT`/`ProfilesIT`: fixture de teste integrado, não é credencial de produção nem é usada fora de `src/test`.
- F-0030, F-0031 — cookie sem `HttpOnly`/`Secure` em `IdentityIT`: o teste cria deliberadamente um cookie adulterado para provar que a sessão é rejeitada; marcar `Secure` impediria o envio em HTTP e anularia o cenário.
- F-0033 — "Modern Web Application": alerta informativo, sem ação requerida.

## Evidência
- `mvn dependency:tree`: tomcat 11.0.27, spring-webmvc 7.0.10, jackson 3.1.7, postgresql 42.7.14, log4j-api 2.25.5, bcprov 1.85.
- SBOM/listagem de dependências resolvidas: 116 artefatos; consulta em lote ao OSV (Maven) em 24/10/2026 retornou **0 vulnerabilidades**.
- Backend: `mvn test` (5 testes) e `mvn verify -Pintegration` com `identity_test` (9 testes) aprovados.
- Frontend: `npm run lint`, `npm run build -- --configuration production`, `npm test` (9 testes) aprovados.
- Navegador: `npm run e2e` (3 cenários) aprovado, incluindo asserção de ausência de violações de CSP; sem erros de console.
- Docker: imagens de backend e frontend reconstruídas; `GET /api/health`, `/.well-known/jwks.json` e SPA em `localhost:4200` respondem 200.
- Cabeçalhos conferidos por `curl`: documento com CSP, COOP, COEP, CORP, Permissions-Policy e `Cache-Control: no-cache`; `/api/*` mantém apenas os cabeçalhos do backend.
- Reexecução do scanner DAST/SCA da esteira não está disponível neste ambiente; a revalidação usou resolução efetiva de dependências + OSV, inspeção de cabeçalhos e testes de navegador. Recomenda-se repetir o scan da esteira sobre este commit.

## Gates
CTX PASS; ANL PASS (dependências e configuração existentes analisadas; sem estrutura nova de aplicação); AUT PASS (pedido humano explícito de correção); QLT PASS (testes backend/frontend/e2e); CLS PASS com riscos residuais registrados acima.

## Pendências e próximos passos
- Repetir SBOM, SCA e DAST na esteira para reavaliar os 34 IDs e encerrar o plano.
- Revisar a periodicidade de atualização do patch do Spring Boot e do BouncyCastle para reduzir a janela de exposição.
- Nenhuma pendência de código conhecida no escopo tratado.

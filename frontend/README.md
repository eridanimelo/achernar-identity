# Frontend
Angular 22; componentes standalone, signals, rotas lazy e arquivos HTML/SCSS/TS separados.

`npm ci` instala as dependências fixadas no lockfile. `npm start` inicia o servidor local com proxy para o backend em :8080. `npm run build -- --configuration production` gera o bundle em dist/identity/browser.

Validação: `npm test`, `npm run lint`. Para testes completos, inicie backend, frontend e Mailpit, configure docker/.env e execute `npx playwright install chromium` e `npm run e2e`. Os testes criam contas com prefixo browser- e excluem essas contas ao concluir; usam o administrador definido no .env. Não execute contra ambiente publicado.

Vite e Piscina possuem overrides para versões corrigidas; reavaliar os overrides ao atualizar @angular/build.

---
name: rule-git-branching
type: rule
description: "Git Branching"
---

# Git Branching

Toda implementação deve ocorrer em branch própria.

Antes de iniciar implementação:
1. mudar para `develop`;
2. atualizar `develop`;
3. criar branch de trabalho a partir de `develop`.

MUST NOT:
- implementar diretamente em `develop`;
- criar branch de implementação a partir de `master`;
- criar branch de implementação a partir de `release`;
- commitar diretamente em `master`;
- commitar diretamente em `release` ou `release/*`;
- executar `git push`;
- executar force push.

O padrão de nome da branch deve seguir `config/git.yaml`.

Quando houver Jira associado, utilizar a chave conforme configuração.

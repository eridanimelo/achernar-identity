---
name: rule-git-commits
type: rule
description: "Git Commits"
---

# Git Commits

Commits devem representar alterações coerentes.

Mensagens devem ser claras e descrever a intenção da alteração.

MUST NOT:
- criar commits com alterações não relacionadas;
- incluir secrets;
- incluir arquivos temporários;
- incluir artefatos gerados desnecessariamente.

Commits locais podem ser realizados quando permitido pela configuração.

MUST NEVER executar `git push`.

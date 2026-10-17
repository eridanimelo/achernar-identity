---
name: corporate-geral-readme
type: docs
description: "Corporate"
---

# Corporate

Pacotes de especialidade responsáveis pelos artefatos da Spec.

Padrão:

```text
corporate/<especialidade>/
├── AGENT.md
├── RULES.md
├── SKILL.md
└── templates/
    └── <artefato>
```

- `AGENT.md` → papel/responsabilidade;
- `RULES.md` → regras normativas;
- `SKILL.md` → processo;
- `templates/` → contrato da saída.

Especialidades atuais:

- Requirements → `business.md`
- API → `api.md`
- Database → `database.md`
- UI/UX → `frontend.md`
- Security → `security.md`
- Test Specification → `tests.md`

Pacotes corporativos recebidos oficialmente são read-only e substituem o pacote
provisório correspondente.

A Spec permanece agnóstica de tecnologia.

---
name: migration
type: skill
description: >-
  Migrar uma feature LEGACY preservando o comportamento funcional relevante:
  coletar evidências AS-IS, separar AS-IS/TO-BE/GAP e registrar divergências.
  Usar em solicitações de migração, reescrita de funcionalidade legada, análise
  AS-IS/TO-BE ou paridade com o sistema antigo.
---

# Skill: Migration

## Quando usar

Quando a feature tiver origem `LEGACY` e precisar ser migrada para o sistema novo.

Se a feature não possuir origem `LEGACY`: `REQUIRES_REVIEW`.

## Quando não usar

Para criar Spec, implementar código ou revisar artefato já especificado sem
foco em migração. Sem origem `LEGACY` comprovada, não migrar.

## Entradas

- `config/paths.yaml` (`legacy.root`) e sistema novo;
- Discovery;
- Spec da feature;
- `rules/migration/migration.md`;
- código legado somente no escopo necessário;
- código do sistema novo quando necessário para reuso.

## Processo

1. localizar evidências funcionais relevantes no legado;
2. confirmar AS-IS;
3. separar AS-IS de TO-BE;
4. atualizar `evidence.md` usando `assets/evidence.md`;
5. atualizar `migration.md` usando `assets/migration.md`;
6. identificar gaps;
7. executar `GATE-ANL` com Reuse Scan no sistema novo (`gates/analise.md`);
8. delegar backend/frontend aos Agents especializados quando houver implementação;
9. revisar paridade somente onde ela for requisito;
10. registrar divergências intencionais.

## Saídas

- `specs/<ID>-<slug>/evidence.md`
- `specs/<ID>-<slug>/migration.md`

## Regras

- MUST NOT inventar comportamento legado;
- MUST NOT copiar a arquitetura antiga automaticamente;
- MUST NOT alterar o código legado durante o discovery;
- código legado é evidência funcional, não modelo técnico;
- MUST usar `REQUIRES_REVIEW` quando faltar evidência crítica.

## Restrições

MUST NOT implementar backend/frontend sem delegação explícita e `GATE-AUT` válido.

## Referências

- `rules/migration/migration.md`
- `rules/reuse/reuse.md`
- `rules/context/context-economy.md`

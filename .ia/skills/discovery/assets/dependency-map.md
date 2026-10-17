---
name: dependency-map
type: skill-asset
description: "Discovery — Dependency Map"
---

# Discovery — Dependency Map

> MELHORIA: recupera o formato simples do modelo antigo e adiciona bloqueio, motivo e impacto em Wave.
> Obrigatório para NEW e LEGACY.

## Dependências entre casos de uso

| Origem | Depende de | Tipo | Bloqueante | Evidência | Motivo | Impacto na ordem/Wave |
|---|---|---|---|---|---|---|

## Tipos

- `FUNCTIONAL`
- `DATA`
- `TECHNICAL`
- `INTEGRATION`

## Cadeias relevantes

Exemplo:

```text
UC-A
  ↓
UC-B
  ↓
UC-C
```

## Casos fundacionais

| Caso de uso | Motivo | Habilita |
|---|---|---|

## Casos independentes
-

## Dependências externas

| Caso de uso | Sistema/Serviço | Tipo | Bloqueante | Impacto |
|---|---|---|---|---|

## Dependências incertas / UNKNOWN

| Origem | Possível dependência | Dúvida | Impacto | Ação necessária |
|---|---|---|---|---|

Dependência incerta que altere ordem de entrega ou Wave MUST gerar `REQUIRES_REVIEW`.

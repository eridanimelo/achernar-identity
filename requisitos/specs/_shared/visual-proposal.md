# Achernar Identity — Proposta Visual e Temas

Sem logo disponível no repositório (premissa A-05, D-38): proposta documentada, substituível por marca oficial sem mudar o sistema de tokens.

## 1. Conceito

**Achernar** é a estrela azul-esbranquiçada de Eridanus, “o fim do rio”. Identidade: **confiável, calma, precisa** — noite profunda + luz fria. Marca tipográfica provisória
“achernar·identity” (peso 600) + ícone de **estrela de quatro pontas** (SVG próprio) com ponto central; versões monocromática e invertida.

```svg
<!-- ícone provisório (viewBox 0 0 24 24) — currentColor -->
<path d="M12 1.5l2.2 7.3 7.3 2.2-7.3 2.2L12 20.5l-2.2-7.3-7.3-2.2 7.3-2.2z" fill="currentColor"/>
```

## 2. Tokens de cor (CSS custom properties; Material 3 mapeado sobre eles)

| Token | Claro | Escuro | Uso |
|---|---|---|---|
| `--ach-bg` | `#F6F8FE` | `#0B1020` | fundo da página |
| `--ach-surface` | `#FFFFFF` | `#121A33` | cartões, diálogos, tabelas |
| `--ach-surface-2` | `#EDF1FC` | `#18223F` | superfícies elevadas / cabeçalho de tabela |
| `--ach-text` | `#0E1630` | `#E8ECF8` | texto principal |
| `--ach-text-2` | `#4A5578` | `#A8B3D6` | texto secundário |
| `--ach-primary` | `#1D4ED8` | `#8FB2FF` | ações primárias, links, foco |
| `--ach-on-primary` | `#FFFFFF` | `#0B1020` | texto sobre primário |
| `--ach-accent` | `#0E7C93` | `#4FD1E8` | realces secundários (estrela/ícones) |
| `--ach-outline` | `#6B7599` | `#7C88B0` | contorno de campos |
| `--ach-error` | `#B3261E` | `#FFB4AB` | erro |
| `--ach-success` | `#1B6E43` | `#7AD9A3` | sucesso |
| `--ach-warning` | `#7A5200` | `#FFC857` | aviso |
| `--ach-focus` | `#1D4ED8` | `#8FB2FF` | anel de foco (2 px + 2 px de offset) |

Gradiente de marca (apenas decorativo, painel das telas de login): `linear-gradient(160deg, #0B1020 0%, #14267A 55%, #1D4ED8 100%)` com “estrelas” em SVG estático (sem animação obrigatória).

### 2.1 Contraste calculado (WCAG 2.x, razão de luminância relativa)

**Tema claro**

| Par | Razão | Exigência |
|---|---|---|
| texto `#0E1630` / fundo `#F6F8FE` | 16,82:1 | ≥ 4,5 ✔ |
| texto `#0E1630` / superfície `#FFFFFF` | 17,86:1 | ≥ 4,5 ✔ |
| texto secundário `#4A5578` / `#FFFFFF` | 7,34:1 | ≥ 4,5 ✔ |
| on-primary `#FFFFFF` / primário `#1D4ED8` | 6,70:1 | ≥ 4,5 ✔ |
| link `#1D4ED8` / `#FFFFFF` | 6,70:1 | ≥ 4,5 ✔ |
| erro `#B3261E` / `#FFFFFF` | 6,54:1 | ≥ 4,5 ✔ |
| sucesso `#1B6E43` / `#FFFFFF` | 6,25:1 | ≥ 4,5 ✔ |
| aviso `#7A5200` / `#FFFFFF` | 6,92:1 | ≥ 4,5 ✔ |
| contorno de campo `#6B7599` / `#FFFFFF` | 4,54:1 | ≥ 3 (UI) ✔ |
| foco `#1D4ED8` / `#FFFFFF` | 6,70:1 | ≥ 3 (UI) ✔ |

**Tema escuro**

| Par | Razão | Exigência |
|---|---|---|
| texto `#E8ECF8` / fundo `#0B1020` | 16,03:1 | ≥ 4,5 ✔ |
| texto `#E8ECF8` / superfície `#121A33` | 14,56:1 | ≥ 4,5 ✔ |
| texto secundário `#A8B3D6` / `#121A33` | 8,26:1 | ≥ 4,5 ✔ |
| on-primary `#0B1020` / primário `#8FB2FF` | 9,00:1 | ≥ 4,5 ✔ |
| link `#8FB2FF` / `#121A33` | 8,17:1 | ≥ 4,5 ✔ |
| erro `#FFB4AB` / `#121A33` | 10,13:1 | ≥ 4,5 ✔ |
| sucesso `#7AD9A3` / `#121A33` | 10,06:1 | ≥ 4,5 ✔ |
| aviso `#FFC857` / `#121A33` | 11,18:1 | ≥ 4,5 ✔ |
| contorno de campo `#7C88B0` / `#121A33` | 4,92:1 | ≥ 3 (UI) ✔ |
| foco `#8FB2FF` / `#121A33` | 8,17:1 | ≥ 3 (UI) ✔ |

(valores calculados na elaboração; `--ach-accent` é decorativo e **não** carrega texto.) A auditoria automatizada (axe) na implementação repete a verificação nos componentes reais.

## 3. Tipografia

| Papel | Fonte (auto-hospedada, sem CDN) | Tamanho/peso |
|---|---|---|
| Títulos | **Manrope** (variável) | Display 32/40 · Título 24/32 · Seção 20/28 · peso 600–700 |
| Texto/UI | **Inter** (variável) | Corpo 16/24 · Pequeno 14/20 · Legenda 12/16 (≥ 12 px) · peso 400/500 |
| Código/segredos | **JetBrains Mono** | 14/20 |
Fallback: `system-ui, -apple-system, "Segoe UI", Roboto, sans-serif`. Fontes WOFF2 *subset* latim; `font-display: swap`; ícones **Material Symbols** (subconjunto auto-hospedado).

## 4. Forma, espaço e elevação

Escala de espaço 4 px (4·8·12·16·24·32·48); raio: campos/botões 12 px, cartões 16 px, diálogos 24 px; contêiner de conteúdo máx. 1200 px, formulários 440 px;
elevação por tonalidade da superfície (M3) com sombra sutil só em diálogos/menus; bordas 1 px `--ach-outline` a 40 % para separação.

## 5. Tema Angular Material (M3)

- Tema próprio definido em `styles/theme.scss` com o mixin de tema do Material 3 (paleta gerada a partir de `#1D4ED8` e neutros azulados), `color-scheme: light dark` e *overrides* de *system tokens* mapeando para as variáveis `--ach-*`.
- Alternância por `data-theme="light|dark"` no `<html>` (quando `system`, sem atributo e vale `prefers-color-scheme`). Script externo `theme-init.js` (mesma origem, sem inline por CSP) aplica o tema antes da pintura para evitar *flash*.
- Densidade padrão 0; botões primários *filled*, secundários *outlined*, terciários *text*; campos *outlined*; tabelas `mat-table` com cabeçalho `--ach-surface-2`; chips de estado com **ícone + texto**.
- Componentes próprios (não só Material): `page-header`, `empty-state`, `secret-reveal`, `password-field`, `status-chip`, painel de marca das telas de login.

## 6. Layouts-chave

```text
LOGIN (≥ 840px)                              SEGURANÇA (≥ 840px)                             APLICAÇÃO (admin)
┌──────────────┬───────────────────┐         ┌ Header: ★ achernar·identity   [Tema][PT][ana▾] ┐   ┌ ★ | Aplicações › meu-app-web   [Salvar] ┐
│ painel de    │  Entrar           │         ├─ Conta ─ Segurança ─ Sessões ─────────────────┤   │ Geral | Origens | Retornos | APIs | Segredos│
│ marca        │  [e-mail       ]  │         │ Senha            [Alterar senha]              │   │ client_id (readonly)  Nome  Tipo  Estado    │
│ (gradiente + │  [senha     👁 ]  │         │ E-mail           ana@…  [Alterar]             │   │ Origens: [+ adicionar]  chips removíveis    │
│  estrela)    │  [ Entrar ]       │         │ Acesso social    Google ✔ [Desvincular]       │   │ Segredos (CONFIDENTIAL) [Gerar novo]        │
│              │  ── ou ──         │         │                  Facebook — [Vincular]        │   └─────────────────────────────────────────────┘
│              │  [Google][Facebook]│        └───────────────────────────────────────────────┘
└──────────────┴───────────────────┘
```

## 7. Movimento e microinterações

Transições de 120–200 ms (opacidade/transform) com `prefers-reduced-motion` desligando-as; *skeleton* com pulso suave (desligável); confirmação de cópia de segredo com mudança de ícone + `aria-live`.

## 8. Diretrizes de conteúdo

Português do Brasil, segunda pessoa neutra (“você”), frases curtas e acionáveis; erros dizem *o que aconteceu* e *o que fazer*, sem culpar nem revelar existência de contas; termos fixos: “Entrar”, “Sair”, “Conta”, “Segurança”, “Sessões”, “Aplicações”, “APIs”, “Chaves”, “Eventos”.

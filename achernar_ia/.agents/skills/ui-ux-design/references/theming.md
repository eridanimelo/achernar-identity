---
name: theming
type: skill-reference
description: "Tema light/dark, cor semântica, tipografia e motion"
---

# Tema, cor e motion

## Tokens semânticos (nunca hex bruto no componente)

`--bg, --surface, --surface-2, --text, --text-muted, --border, --primary, --on-primary, --accent, --success, --warning, --danger`.

Light e dark são duas atribuições desses tokens, não dois CSS separados. Testar contraste AA nos dois (texto normal 4.5:1, grande/ícone 3:1). Nunca comunicar estado só por cor — sempre texto ou ícone junto.

## Diretrizes

- Fundo neutro; cor só para hierarquia ou estado;
- dark: elevar superfície por luminosidade, não por sombra; reduzir saturação de cores vibrantes; imagens com leve dim;
- tipografia: corpo >= 16px, line-height ~1.5, headlines com tracking levemente negativo;
- ícones: uma única biblioteca (Lucide, Phosphor ou Material Symbols), SVG, nunca emoji funcional;
- bordas 1px só quando estruturais; preferir espaçamento e elevação tonal sutil.

## Motion

150-300ms com easing, comunica causa ou espaço (de onde veio, para onde vai). Concentrar em momentos de alto impacto (carregamento inicial com stagger, transição de estado relevante). Respeitar `prefers-reduced-motion`. Nunca animar `width/height` em loop; preferir `transform/opacity`.

## Persistência do tema

Respeitar `prefers-color-scheme` como default, permitir override manual persistido, transição sem flash (aplicar classe no `<html>` antes do paint).

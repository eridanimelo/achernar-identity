---
name: components
type: skill-reference
description: "Padrões para tables, charts, chat, canvas, drag-drop e PWA"
---

# Componentes modernos

## Tables

Cabeçalho sticky, ordenação e filtro descobríveis, paginação para volume grande, estado vazio que orienta próximo passo, ações secundárias em menu de contexto. Densidade confortável (padding vertical ~12-16px). Mobile segue `responsive.md`.

## Charts

Sempre legenda, tooltip e eixo legível; paleta acessível (não só cor — usar forma/label); número com contexto (unidade, período); estado vazio/erro desenhado; evitar 3D e animação puramente decorativa. Para dashboard: pergunta primeiro, gráfico depois — cada chart responde uma decisão.

## Chat

Streaming com estado (enviando, erro com retry), markdown seguro, lista de mensagens com paginação reversa, input com enter-para-enviar + shift-enter quebra, acessibilidade (live region para novas mensagens), offline com fila. Nunca bloquear a tela durante resposta.

## Canvas / whiteboard

Camadas (pan, zoom, undo/redo), toolbar contextual mínima, atalhos de teclado, autosave com indicador, exportação (PNG/SVG). Toque e mouse convivem; alvos >= 44px mesmo no canvas UI.

## Drag-drop

Handle visível, feedback durante arrasto (placeholder, elevação), alternativa por teclado/botões (mover para cima/baixo), persistência otimista com rollback em erro, respeitar `prefers-reduced-motion` (sem animação essencial só por drag).

## PWA

Manifesto + ícones maskable, service worker com estratégia por tipo (cache-first para estáticos, network-first para dados), página offline útil, instalabilidade sem forçar, push só com opt-in explícito. Performance: lazy-load, WebP/AVIF, CLS < 0.1, LCP priorizado.

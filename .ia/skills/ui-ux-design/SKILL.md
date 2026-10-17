---
name: ui-ux-design
description: Projetar interfaces modernas, responsivas e acessíveis (fluxos, responsividade, light/dark, PWA, tables, charts, chat, canvas, drag-drop). Usar ao desenhar, revisar ou implementar telas, componentes e fluxos de frontend.
---

# UI/UX Design

## Quando usar

Quando for desenhar, revisar ou implementar interface: nova tela, fluxo, componente (table, chart, chat, canvas, drag-drop), responsividade, tema light/dark, PWA ou auditoria visual.

## Quando não usar

Para regra de negócio, contrato de API, schema de banco ou teste automatizado. Não substitui `agents/frontend/AGENT.md` (contrato de contexto) nem `skills/frontend/SKILL.md` (procedimento de implementação).

## Entradas

- Objetivo da tela e ação primária;
- perfil de uso (mobile predominante ou desktop operacional, sessão curta ou longa);
- Spec alvo (`business.md`, `frontend.md`) quando existir;
- tecnologia alvo (`config/technology.yaml`, chave `frontend`);
- componentes e tokens já existentes no projeto.

## Processo

1. Entender finalidade: objetivo do usuário, objetivo do negócio, ação primária, ações secundárias, frequência e criticidade;
2. Mapear dependências entre tarefas: listar passos, pré-requisitos e estados (vazio, carregando, erro, sucesso, sem-resultado) antes de escolher layout;
3. Escolher padrão de fluxo por dependência (ver `references/flows.md`);
4. Definir hierarquia visual: onde estou, objetivo, conteúdo principal, ação primária, ações secundárias;
5. Compor responsivo desde o início (ver `references/responsive.md`): o que fica lado a lado, o que empilha, o que vira detalhe, o que permanece acessível;
6. Aplicar tema light/dark por tokens semânticos (ver `references/theming.md`);
7. Escolher componentes pelo problema, reutilizando o existente (ver `references/components.md`): table, chart, chat, canvas, drag-drop, PWA;
8. Reduzir ruído: progressive disclosure, cor com propósito, sem texto redundante, espaço como estrutura;
9. Rodar checklist de auditoria antes de entregar.

## Checklist de auditoria

- [ ] Ação primária inequívoca, secundárias subordinadas;
- [ ] todos os estados desenhados (vazio, loading, erro, sucesso);
- [ ] contraste AA, foco visível, operável por teclado;
- [ ] toques >= 44x44px, sem dependência só de hover ou só de cor;
- [ ] erros específicos e acionáveis junto ao campo;
- [ ] sem scroll horizontal indesejado em nenhum breakpoint;
- [ ] tabelas CRUD com fallback vertical no mobile;
- [ ] light/dark legível nos dois temas, sem hex bruto no componente;
- [ ] motion 150-300ms, com `prefers-reduced-motion` respeitado;
- [ ] padrão conferido contra telas equivalentes existentes.

## Saídas

- Composição validada (fluxo, hierarquia, responsivo, estados);
- padrão escolhido + motivo + alternativa descartada (quando não óbvio);
- comportamento desktop e mobile declarados;
- lista de reuso (o que foi reutilizado, composto, estendido, criado).

## Referências

Carregar sob demanda, só a necessária ao escopo:

- `references/responsive.md` — quando houver layout, breakpoint, tabela ou formulário;
- `references/flows.md` — quando houver sequência, dependência entre etapas ou escolha tabs/stepper/wizard/modal/drawer;
- `references/theming.md` — quando houver cor, light/dark, tipografia ou motion;
- `references/components.md` — quando houver table, chart, chat, canvas, drag-drop ou PWA.

Se faltar decisão crítica: `REQUIRES_REVIEW`.

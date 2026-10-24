---
name: docs-auditoria-fluxo-2026-10-24
type: docs
description: "Auditoria do fluxo do motor e plano priorizado de melhorias"
---

# Auditoria do fluxo — 24/10/2026

Escopo: orquestração, contratos dos Agents, Skills, gates, estado e portabilidade.
Revisão estática dos arquivos relacionados; não é validação funcional do produto
nem execução completa de todas as intenções. Origem NEW/LEGACY não se aplica à
manutenção do motor. Nenhum pacote corporativo foi alterado.

Resultado: REQUIRES_REVIEW para as decisões estruturais propostas abaixo.
Duas inconsistências objetivas foram corrigidas nesta unidade.

## Descoberta e seleção de Skills

As Skills locais aparecem no catálogo desta sessão. No Codex, a descoberta
carrega nome, descrição e caminho; o corpo é lido quando a Skill é selecionada.
A seleção implícita depende da descrição e não garante execução de toda
capacidade obrigatória do projeto. Portanto, não remover todas as referências.

Recomendação: manter uma indicação normativa por capacidade obrigatória no
Agent; eliminar referências repetidas que apenas reproduzem a mesma obrigação.
Skills auxiliares podem depender da descoberta implícita. Os arquivos
`subagents/*/AGENT.md` são contratos locais carregados pelo orquestrador;
o nome do diretório não os transforma em agentes executáveis automaticamente.
O mesmo cuidado vale para procedimentos em `corporate/*/SKILL.md`, fora do
diretório local de descoberta de Skills.

Fonte: [documentação oficial de Skills](https://learn.chatgpt.com/docs/build-skills).

## Correções aplicadas

1. `subagents/specification/AGENT.md`: conclusão da Spec usa critérios de
   consistência/aprovação; GATE-AUT passa a ser explicitamente avaliado somente
   na entrada de implementação solicitada. Antes, o contrato mandava avaliá-lo
   ao concluir a Spec, embora o gate exigisse intenção de implementar.
2. `workflows/feature.md`: DATABASE deixa de figurar como implementador;
   seu contrato proíbe código e define saída lógica. Backend executa migrations
   conforme a Spec. GATE-AUT agora aparece explicitamente na entrada da
   implementação, ao lado de GATE-ANL.

## Melhorias priorizadas

| Prioridade | Evidência | Problema e proposta |
|---|---|---|
| Alta | `rules/context/session-lifecycle.md`, seção Mudança de contexto | Exigir nova sessão em toda mudança de intenção/fase pode interromper pedidos amplos já autorizados. Distinguir limite de autorização de necessidade de isolamento; checkpoint deve permitir retomada. Preservar execução de uma etapa quando esse for o escopo humano. |
| Alta | `AGENTS.md`, roteamento | Não há intenção explícita para manutenção do próprio motor. Definir rota de manutenção ou operação de Review que possa aplicar correções expressamente autorizadas, com contexto e validação próprios. Evitar forçar mudanças do framework a Specs de produto. |
| Alta | `.agents/skills/specification/assets/status.yaml` | Aprovação tem apenas um status. Registrar responsável, evidência da decisão e revisão aprovada; alterações materiais devem invalidar aprovações e gates dependentes. |
| Alta | `rules/context/session-lifecycle.md`, `gates/README.md` | Há vários arquivos de estado sem protocolo de atualização/conflito. Definir fonte canônica por feature, tratar índices como projeções e atualizar com escrita atômica e controle de revisão. Um current global não deve ser fonte exclusiva para execuções concorrentes. |
| Alta | `config/paths.yaml`; ausência observada de `current.yaml` no local configurado | Definir inicialização: ausência de estado em projeto novo não é falha; criar estado apenas ao executar etapa que o exija. Separar ausência esperada de estado inválido. |
| Alta | `subagents/database/AGENT.md`, `gates/analise.md` | Mesmo após corrigir o workflow, DATABASE cita gate anterior a mudança de schema, enquanto proíbe código de implementação. Delimitar análise de modelo lógico versus execução física; esta última deve cumprir os gates de implementação. |
| Média | `gates/qualidade.md`, `gates/analise.md`, `.agents/skills/review/SKILL.md` | Regras de Angular/Java estão copiadas em várias camadas. Manter a norma na Rule e referenciar sua aplicabilidade no gate/Skill. Evitar divergência entre cópias. |
| Média | `.agents/skills/specification/SKILL.md`, modos REQUIREMENTS e FULL_SPEC | REQUIREMENTS sempre retorna REQUIRES_REVIEW, mesmo sem decisão crítica; FULL_SPEC condiciona o resultado à pendência. Separar sucesso da etapa de aprovação humana pendente. |
| Média | `rules/specification/spec-structure.md`, template `status.yaml` | Estados semânticos em maiúsculas coexistem com estados de artefato, aprovação e gate em minúsculas. Não são necessariamente o mesmo domínio: documentar enums por campo e validar por schema, evitando conversão indiscriminada. |
| Média | template `context-manifest.yaml` | O manifesto começa com leituras de Requirements e proibições fixas, mas é usado em várias intenções. Usar base neutra e registrar separadamente leituras planejadas e realizadas; preservar pré-condições sem ciclo entre ler e registrar a leitura. |
| Média | `AGENTS.md`, `config/paths.yaml`, permissões desta sessão | Produto e estado estão fora da raiz gravável do motor nesta sessão. Documentar configuração de acesso às raízes resolvidas; descoberta de instruções e autorização de escrita são mecanismos distintos. Não mover estado para dentro do motor apenas para contornar permissões. |
| Média | `rules/context/session-lifecycle.md`, regras de Reuse Scan | Separar contrato de especialista de mecanismo de execução. Oferecer fallback explícito quando o harness não suportar delegação ou criação de sessão; não simular isolamento apenas lendo outro Markdown. |
| Média | inventário de scripts locais | Foram localizados geradores de índice e grafo, mas nenhum validador geral do motor no inventário consultado. Criar verificação determinística de YAML, rotas, referências, contratos, enums, paths e transições. |
| Média | `AGENTS.md`, documentação de arquitetura | O escopo inicial acumula roteamento, exemplos extensos de layout e convenções. Reduzir exemplos no arquivo inicial e preservar nele apenas invariantes, resolução de paths e pontos de entrada. |
| Baixa | descrições das Skills locais | Backend, Frontend, Migration e Specification têm áreas próximas. Avaliar prompts positivos e negativos para medir seleção correta; antecipar gatilhos e limites na descrição. Não criar Skills adicionais sem capacidade distinta. |

## Fluxo recomendado

Solicitação → intenção principal e operação → escopo autorizado → resolução de
paths e estado → contexto obrigatório → validação das pré-condições → execução
solicitada → verificações proporcionais → checkpoint → resultado e pendências.

Discovery organiza escopo e dependências; Requirements consolida comportamento;
Specification define contratos aplicáveis; implementação exige aprovação e
autorização; testes e segurança participam durante implementação conforme o
risco. Review verifica evidências e não substitui testes executados.

Manter a parada por etapa quando esse for o pedido. Para autorização humana
mais ampla, propor política explícita de transições permitidas, em vez de
pressupor autorização ou exigir repetição de uma autorização já concedida.

## Próximas unidades sugeridas

1. Definir manutenção do motor, sucesso de etapa versus aprovação e lifecycle.
2. Formalizar schemas, revisão da aprovação, inicialização e atualização do estado.
3. Remover duplicações normativas e tornar o manifesto neutro por intenção.
4. Criar validador e suíte de cenários de roteamento e transição.

Cenários mínimos: projeto sem estado; feature NEW em produto existente;
LEGACY com evidência insuficiente; pedido só de requisito; pedido só de artefato;
Spec concluída sem autorização de implementação; correção pequena autorizada;
retomada após interrupção; aprovação invalidada por mudança material; duas
features concorrentes; harness sem subagentes; escrita fora da raiz permitida.

## Validação desta unidade

GATE-CTX: PASS para manutenção documental de escopo delimitado; fontes do motor
relacionadas foram lidas. Manifesto de Spec não aplicável.
GATE-ANL e GATE-AUT: não aplicáveis à alteração documental do motor.
GATE-QLT: conferência estática do diff e de whitespace; sem código de produto.
GATE-CLS: alterações limitadas às duas inconsistências e a este relatório;
decisões estruturais permanecem propostas, não políticas já aprovadas.
Registro no próprio motor para manter esta auditoria de manutenção independente
do estado de features; o destino externo configurado não foi escrito.

## Segunda rodada — revisão operacional

Escopo adicional: implementação dos geradores, revisão de impacto, configuração
Git/Jira e coerência dos gates. Esta rodada acrescenta findings; não aplica
correções de código nem muda as políticas operacionais.

### Achados confirmados por reprodução

1. **Cache pode permanecer incorreto após exclusão.** Em
   `reuse-index/scripts/build_index.py -> is_stale` e
   `graph-view/scripts/build_graph.py -> is_stale`, a comparação considera os
   arquivos ainda existentes e seus mtimes. Uma reprodução com arquivo antigo,
   saída posterior e exclusão do arquivo retornou `False` nos dois scripts.
   Proposta: guardar inventário de entradas e assinatura da configuração e do
   gerador; comparar adições, remoções e alterações. Incluir teste de rename,
   checkout com timestamps preservados e mudança da configuração. Para o índice,
   `is_stale` também não verifica alteração de paths/technology nem do gerador.
2. **Raiz entre aspas é interpretada incorretamente.** No índice,
   `_read_project_root` usa regex e envia o valor diretamente para `Path`.
   Com YAML válido `root: ".."`, a reprodução retornou um diretório cujo nome
   contém as aspas. O grafo usa a mesma estratégia nessa função. Proposta:
   unificar leitura tipada da configuração e validar os caminhos antes de
   qualquer varredura ou escrita; testar aspas, espaços e comentários.

Reproduções executadas com imports dos scripts e arquivos temporários;
configurações e caches reais não foram alterados. Os resultados comprovam esses
casos específicos, não a correção geral dos scripts.

### Outros ajustes necessários

| Prioridade | Evidência | Ajuste recomendado |
|---|---|---|
| Alta | `subagents/review/AGENT.md`: somente código efetivamente alterado | Permitir consumidores, contratos, configuração e testes diretamente relacionados ao impacto, mesmo sem diff. Limitar a expansão por hipótese de risco explícita; evitar revisão cega ao entorno. |
| Alta | `gates/encerramento.md` exige GATE-QLT; `gates/qualidade.md` cobre implementação | Definir encerramento documental com verificações próprias e QLT não aplicável quando não houver implementação. Atualmente uma conclusão de Discovery/Spec pode ficar dependente de um gate fora de seu escopo. |
| Alta | `rules/git/branching.md`: trocar/atualizar develop antes de implementar | Inspecionar branch e alterações locais primeiro; reutilizar branch adequada em retomadas. Não trocar branch ou atualizar base indiscriminadamente com trabalho em curso. Separar início de feature de continuação. |
| Alta | `config/PROJECT.yaml` identifica Achernar Identity; `config/jira.yaml` identifica TSE-CSADM-CERES-SOFTWARE e Cerimonial TSE | Confirmar se a associação é intencional antes de escrita externa. Projeto válido no MCP pode ainda ser o projeto errado para este produto; não inferir os novos valores. |
| Média | `graph-view/scripts/build_graph.py -> collect_real_files` exclui YAML/YML | A descrição anuncia grafo de config e estado, mas esses arquivos não aparecem como nós. Exibir configuração/estado como nós ou declarar a exclusão na capacidade e na visualização. |
| Média | `graph-view/scripts/build_graph.py -> WALK_TARGETS` usa primeiro segmento das raízes | Uma raiz aninhada pode ampliar a varredura a diretórios irmãos. Usar raízes exatas, deduplicar sobreposições e podar cache e diretórios gerados antes de percorrer. |
| Média | `norm_rel` aceita absolutos; resolução de project.root aceita absolutos | Scripts aceitam configuração que AGENTS proíbe. Validar contrato de paths e rejeitar valores inválidos com mensagem precisa; project.root continua sendo resolvido relativamente ao motor. |
| Média | `config/jira.yaml -> specPathPattern` duplica prefixo de workspace.specs | Derivar o prefixo da configuração de paths ou validar igualdade automaticamente. O espelho atual exige duas edições e enfraquece a fonte única de paths. |
| Média | Workflow, etapa Validação; contrato Testing atribui execução a Backend/Frontend | Diferenciar execução real de testes, análise de segurança e revisão de evidências na tabela. Evitar que um relatório de Testing seja confundido com execução. |
| Média | `rules/specification/spec-structure.md` proíbe qualquer biblioteca/banco na Spec | Avaliar exceções para restrições humanas verificáveis de compatibilidade e integração. Manter decisões de implementação em config/ADR, mas preservar requisitos externos que dependem de protocolo, fornecedor ou versão. |
| Média | Múltiplos docs de diagnóstico/summary e auditoria no motor | Classificar documentos como normativos, históricos ou propostas. Referências textuais nesses documentos podem gerar arestas de grafo que parecem dependências operacionais atuais. |

### Retificação de recomendação anterior

`rules/reuse/reuse.md` já define fallback sem subagentes e dispensa delegação
quando não há decisão de reuso. Logo, não falta criar essa política do zero.
O ajuste é alinhar os Agents que exigem delegação e explicar que executar o
contrato na sessão atual não cria isolamento real. A criação de nova sessão
continua sendo questão separada de lifecycle.

### Ordem recomendada após esta rodada

1. Corrigir invalidação de cache e leitura/resolução de configuração com testes
   de regressão reproduzindo os defeitos acima.
2. Ajustar alcance de Review, encerramento sem implementação e retomada Git.
3. Resolver o vínculo Jira antes de operações externas.
4. Consolidar estados, aprovações e manutenção do motor conforme a primeira rodada.

Não acrescentar mais camadas de Agents ou gates antes de resolver essas lacunas.
O principal ganho agora é tornar as regras existentes consistentes e verificáveis.

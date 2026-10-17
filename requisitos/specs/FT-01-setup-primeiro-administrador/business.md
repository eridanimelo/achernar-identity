# Business Spec — FT-01 Setup do primeiro administrador

## 0. Identificação
- ID: FT-01 (UC-001)
- Nome: Setup do primeiro administrador
- Origem: `NEW`
- Módulo/Domínio: M1 Setup e primeiro administrador
- Status: `APPROVED` (aprovação documental sob delegação do prompt de 2026-10-16; **não** autoriza implementação)

## 1. Objetivo
Permitir que o operador de uma instalação nova crie o **primeiro administrador** sem que exista usuário, senha ou token fixo no código, na imagem ou na configuração versionada. A criação exige um segredo de implantação conhecido apenas pelo operador e só é possível enquanto não houver administrador ativo.

## 2. História / Necessidade
**Como** operador da instalação **quero** criar o primeiro administrador usando um segredo que eu forneci na implantação **para** poder administrar contas, aplicações e APIs sem credenciais embutidas no produto.

## 3. Fluxo funcional

### UC-001 — Criar o primeiro administrador
**Fluxo principal**
1. O operador define o segredo de implantação (≥ 32 caracteres aleatórios) fora do repositório e inicia o serviço.
2. O operador abre a tela de setup. O sistema informa se o setup é necessário (não existe administrador ativo) e está disponível (segredo configurado).
3. O operador informa o segredo de implantação, o e-mail e a senha (com confirmação).
4. O sistema limita as tentativas, compara o segredo em tempo constante, valida e-mail e senha pela política de senhas e verifica que o e-mail não pertence a outra conta.
5. O sistema cria a conta com perfil administrador, ativa e com e-mail considerado verificado, registra o evento de criação e encerra a disponibilidade do setup.
6. O operador é direcionado ao login; a sessão só existe após o login normal.

**Fluxos alternativos**
- A1. Já existe administrador ativo: o setup responde como inexistente; a tela redireciona ao login.
- A2. Não há administrador ativo e o segredo de implantação não está definido: o setup fica indisponível e o serviço avisa no registro de inicialização (em produção a inicialização falha).
- A3. **Recuperação:** todos os administradores existentes estão inativos (ex.: alteração direta do banco): o setup volta a ficar disponível com o mesmo segredo.
- A4. Tentativas excedidas: o sistema recusa temporariamente e informa quando tentar de novo.

**Exceções conhecidas**
- E1. Segredo incorreto ⇒ recusa genérica, evento de falha, nenhum dado criado.
- E2. Senha fora da política ou e-mail inválido ⇒ erros por campo.
- E3. E-mail já cadastrado ⇒ conflito.
- E4. Duas solicitações simultâneas ⇒ no máximo um administrador é criado.

## 4. Critérios de aceite

### CA-FT01-01 — Setup disponível
- **Dado** que não existe administrador ativo e o segredo de implantação está configurado
- **Quando** o operador consulta o estado do setup
- **Então** o sistema informa que o setup é necessário e está disponível.
**Referências:** RF-15, RN-SET-01, RN-SET-04

### CA-FT01-02 — Criação do primeiro administrador
- **Dado** o setup disponível
- **Quando** o operador envia segredo correto, e-mail válido e senha conforme a política
- **Então** é criada uma conta administradora ativa com e-mail verificado, nenhum token é emitido, o evento `BOOTSTRAP_ADMIN_CREATED` é registrado e o setup deixa de estar disponível.
**Referências:** RF-15, RN-SET-03, RN-SET-07, RN-PWD-01

### CA-FT01-03 — Segredo incorreto
- **Dado** o setup disponível
- **Quando** o segredo informado é incorreto ou ausente
- **Então** o sistema responde `SETUP_TOKEN_INVALID` sem criar conta, registra `SETUP_ATTEMPT_FAILED` e conta a tentativa no limite.
**Referências:** RN-SET-02

### CA-FT01-04 — Limite de tentativas
- **Dado** 5 tentativas inválidas em 15 minutos pelo mesmo endereço
- **Quando** ocorre nova tentativa (mesmo com segredo correto)
- **Então** o sistema responde `RATE_LIMITED` com `Retry-After`.
**Referências:** RN-SET-02, P-38

### CA-FT01-05 — Setup encerrado
- **Dado** que existe administrador ativo
- **Quando** qualquer requisição de criação ou consulta de criação é feita (mesmo com segredo correto)
- **Então** o sistema responde `SETUP_NOT_AVAILABLE` e não altera dados.
**Referências:** RN-SET-01

### CA-FT01-06 — Sem segredo de implantação
- **Dado** nenhum administrador ativo e segredo não configurado
- **Quando** o serviço inicia e o operador consulta o estado
- **Então** o setup consta como indisponível, o registro de inicialização contém aviso (e em produção a inicialização é recusada).
**Referências:** RN-SET-04

### CA-FT01-07 — Validação
- **Dado** o setup disponível e segredo correto
- **Quando** a senha viola a política ou o e-mail é inválido
- **Então** o sistema devolve erros por campo e não cria conta.
**Referências:** RN-PWD-01, RN-ACC-02

### CA-FT01-08 — Recuperação sem administrador ativo
- **Dado** que existe conta administradora apenas desativada
- **Quando** o operador usa o setup com o segredo correto
- **Então** é criada nova conta administradora e o setup se encerra.
**Referências:** RN-SET-01, D-24

### CA-FT01-09 — E-mail já cadastrado
- **Dado** setup disponível e segredo correto
- **Quando** o e-mail já pertence a uma conta
- **Então** o sistema responde `CONFLICT` sem alterar a conta existente.
**Referências:** RN-SET-06, RN-ACC-02

### CA-FT01-10 — Ausência de credenciais fixas
- **Dado** o repositório, as imagens e a configuração de exemplo
- **Quando** inspecionados
- **Então** não existe senha, token ou e-mail de administrador real; apenas placeholders vazios.
**Referências:** RN-SET-05, RN-EML-08

### CA-FT01-11 — Concorrência
- **Dado** duas solicitações válidas simultâneas
- **Quando** ambas são processadas
- **Então** exatamente uma cria o administrador e a outra recebe `SETUP_NOT_AVAILABLE` ou `CONFLICT`.
**Referências:** RN-SET-01, RNF-20

### CA-FT01-12 — Aplicação da interface semeada
- **Dado** a primeira inicialização
- **Quando** o serviço conclui a inicialização
- **Então** existe a aplicação própria da interface do Identity registrada (origens e URLs de configuração) e o administrador consegue entrar por ela.
**Referências:** RN-SET-05, D-46

## 5. Regras de negócio
| ID | Regra | Origem/Referência | Situação |
|---|---|---|---|
| RN-SET-01 | Setup só enquanto não existir administrador ativo | §3 | CONFIRMED |
| RN-SET-02 | Segredo de implantação comparado em tempo constante; limite de tentativas; evento em falha | §3 | CONFIRMED |
| RN-SET-03 | Cria conta ativa, e-mail verificado, perfil administrador, evento; sem sessão | §3 | CONFIRMED |
| RN-SET-04 | Sem segredo e sem admin: indisponível + aviso (prod: falha de inicialização) | §3 | CONFIRMED |
| RN-SET-05 | Primeira inicialização semeia a aplicação da interface; sem credenciais fixas | §3 | CONFIRMED |
| RN-SET-06 | E-mail já cadastrado ⇒ conflito | §3 | INFERRED |
| RN-SET-07 | Setup não cria sessão | §3 | INFERRED |
| RN-PWD-01 | Política de senha | §8 | CONFIRMED |
| RN-ACC-02 | E-mail normalizado e único | §4 | CONFIRMED |

## 6. Dados funcionais
| Campo/Dado | Significado | Obrigatório | Formato/Restrição funcional | Regra |
|---|---|---|---|---|
| segredo de implantação | prova de que o solicitante é o operador | sim | ≥ 32 caracteres; nunca exibido nem registrado | RN-SET-02 |
| e-mail | identificação do administrador | sim | endereço válido, ≤ 254 caracteres, normalizado | RN-ACC-02 |
| senha | credencial do administrador | sim | política de senhas | RN-PWD-01 |
| idioma | idioma das comunicações | não | `pt-BR` ou `en` (padrão do ambiente) | RN-ACC-08 |

## 7. Ações funcionais
| Ação | Pré-condição | Resultado esperado | Regra |
|---|---|---|---|
| Consultar estado do setup | nenhuma | informa necessário/disponível | RN-SET-01/04 |
| Criar primeiro administrador | setup disponível; segredo correto | administrador criado; setup encerrado | RN-SET-03 |

## 8. Perfis e permissões funcionais
| Perfil/Ator | Capacidade | Restrição | Referência |
|---|---|---|---|
| Operador (quem tem o segredo) | criar o primeiro administrador | somente sem administrador ativo | RN-SET-01 |
| Visitante sem segredo | consultar estado | nada além de “necessário/disponível” | RN-SET-02 |

## 9. Rastreabilidade
| Item | Origem/Referência | Situação |
|---|---|---|
| UC-001 | RF-15, D-24 | CONFIRMED |
| CA-FT01-01…12 | RF-15, RN-SET-*, RN-PWD-01 | CONFIRMED |

## 10. Pontos a validar
Nenhum bloqueante. Premissa A-06: o operador gera e guarda o segredo de implantação em cofre.

## 11. Divergências
| ID | Assunto | Fonte A | Fonte B | Divergência | Ação necessária |
|---|---|---|---|---|---|
| DEV-03 | Descrição do projeto | `PROJECT.yaml` (SSO) | prompt | produto sem SSO | atualização registrada em `config-changes.md` |

## 12. LEGACY — AS-IS / TO-BE / GAP
`NOT_APPLICABLE` (origem NEW).

## 13. Resultado esperado
Existe um único caminho seguro para criar o primeiro administrador, impossível de reutilizar depois, sem credenciais embutidas.

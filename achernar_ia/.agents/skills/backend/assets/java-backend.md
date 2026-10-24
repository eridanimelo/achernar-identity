---
name: java-backend
type: skill-asset
description: "Template Java de backend"
---

# Template Java de backend

Referência de forma para implementação Java/Spring. Fonte normativa:
`rules/backend/java-backend.md`. A Spec aprovada é a fonte de comportamento.

`Pessoa` é exemplo didático. Adaptar pacote (via `config/paths.yaml`,
`backend.root`), nomes, campos, DTOs e consultas a cada funcionalidade.
MUST NOT criar CRUDs, campos ou relacionamentos sem requisito.

## Estrutura por funcionalidade

| Caminho relativo a `src/main/java/<pacote-base>/` | Responsabilidade |
|---|---|
| pessoa/controller/PessoaController.java | HTTP e validação de entrada |
| pessoa/dto/PessoaCriacaoRequest.java | Dados permitidos na criação |
| pessoa/dto/PessoaResponse.java | Campos exigidos na resposta |
| pessoa/entity/Pessoa.java | Persistência JPA |
| pessoa/repository/PessoaRepository.java | Consultas explícitas |
| pessoa/service/PessoaService.java | Contrato do serviço |
| pessoa/service/impl/PessoaServiceImpl.java | Orquestração e transações |
| pessoa/exception/PessoaNaoEncontradaException.java | Erro específico |
| shared/exception/ApiExceptionHandler.java | Tradução global para HTTP |
| src/main/resources/db/changelog/ (a partir do backend) | Migrações Liquibase |

## Entity

Arquivo: `pessoa/entity/Pessoa.java`

```java
package br.com.achernar.identity.pessoa.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "pessoa")
public class Pessoa {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String nome;

    protected Pessoa() { }

    public Pessoa(String nome) {
        this.nome = nome;
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
}
```

Constraints ilustrativas; substituir pelas decisões aprovadas.

Relação exigida pelo domínio (requer entidade e migração próprias, sem
coleção inversa):

```java
@ManyToOne(fetch = FetchType.LAZY)
@JoinColumn(name = "organizacao_id")
private Organizacao organizacao;
```

## DTOs

Arquivo: `pessoa/dto/PessoaCriacaoRequest.java`

```java
package br.com.achernar.identity.pessoa.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PessoaCriacaoRequest(
    @NotBlank @Size(max = 120) String nome
) { }
```

Arquivo: `pessoa/dto/PessoaResponse.java`

```java
package br.com.achernar.identity.pessoa.dto;

public record PessoaResponse(Long id, String nome) { }
```

Somente campos exigidos; sem entidades JPA, grafos completos ou segredos.

## Repository

Arquivo: `pessoa/repository/PessoaRepository.java`

```java
package br.com.achernar.identity.pessoa.repository;

import br.com.achernar.identity.pessoa.dto.PessoaResponse;
import br.com.achernar.identity.pessoa.entity.Pessoa;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PessoaRepository extends JpaRepository<Pessoa, Long> {
    @Query("""
        SELECT new br.com.achernar.identity.pessoa.dto.PessoaResponse(p.id, p.nome)
        FROM Pessoa p
        WHERE p.id = :id
        """)
    Optional<PessoaResponse> consultarRespostaPorId(@Param("id") Long id);

    // Usar apenas quando o caso de uso exigir a entidade, como uma alteração.
    @Query("SELECT p FROM Pessoa p WHERE p.id = :id")
    Optional<Pessoa> consultarEntidadePorId(@Param("id") Long id);
}
```

JPQL usa o nome da entidade (`Pessoa`); projeção por construtor com campos
explícitos. Alternativa SQL nativa somente com justificativa:

```java
@Query(value = "SELECT p.id, p.nome FROM pessoa p WHERE p.id = :id", nativeQuery = true)
Optional<Pessoa> consultarEntidadePorIdNativo(@Param("id") Long id);
```

Métodos herdados têm transação própria: não chamar `findById`/`findAll`
esperando consulta sem transação. `save` permitido para escrita na transação
do serviço.

## Service

Arquivo: `pessoa/service/PessoaService.java`

```java
package br.com.achernar.identity.pessoa.service;

import br.com.achernar.identity.pessoa.dto.PessoaCriacaoRequest;
import br.com.achernar.identity.pessoa.dto.PessoaResponse;

public interface PessoaService {
    PessoaResponse consultarPorId(Long id);
    PessoaResponse criar(PessoaCriacaoRequest request);
}
```

## ServiceImpl e conversão manual

Arquivo: `pessoa/service/impl/PessoaServiceImpl.java`

```java
package br.com.achernar.identity.pessoa.service.impl;

import br.com.achernar.identity.pessoa.dto.*;
import br.com.achernar.identity.pessoa.entity.Pessoa;
import br.com.achernar.identity.pessoa.exception.PessoaNaoEncontradaException;
import br.com.achernar.identity.pessoa.repository.PessoaRepository;
import br.com.achernar.identity.pessoa.service.PessoaService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PessoaServiceImpl implements PessoaService {
    private final PessoaRepository repository;

    public PessoaServiceImpl(PessoaRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public PessoaResponse consultarPorId(Long id) {
        return repository.consultarRespostaPorId(id)
            .orElseThrow(() -> new PessoaNaoEncontradaException(id));
    }

    @Override
    @Transactional
    public PessoaResponse criar(PessoaCriacaoRequest request) {
        Pessoa pessoa = paraEntidade(request);
        Pessoa salva = repository.save(pessoa);
        return paraResposta(salva);
    }

    private Pessoa paraEntidade(PessoaCriacaoRequest request) {
        return new Pessoa(request.nome());
    }

    private PessoaResponse paraResposta(Pessoa pessoa) {
        return new PessoaResponse(pessoa.getId(), pessoa.getNome());
    }
}
```

Com lazy, preferir projeções ou fetch explícito; sem Open Session in View.

## Controller

Arquivo: `pessoa/controller/PessoaController.java`

```java
package br.com.achernar.identity.pessoa.controller;

import br.com.achernar.identity.pessoa.dto.*;
import br.com.achernar.identity.pessoa.service.PessoaService;
import jakarta.validation.Valid;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pessoas")
public class PessoaController {
    private final PessoaService service;

    public PessoaController(PessoaService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public PessoaResponse consultar(@PathVariable("id") Long id) {
        return service.consultarPorId(id);
    }

    @PostMapping
    public ResponseEntity<PessoaResponse> criar(
            @Valid @RequestBody PessoaCriacaoRequest request) {
        PessoaResponse response = service.criar(request);
        return ResponseEntity.created(URI.create("/api/pessoas/" + response.id()))
            .body(response);
    }
}
```

Autorização definida pelo caso de uso e segurança; sem liberação pública
implícita.

## Exceptions e tratamento global

Arquivo: `pessoa/exception/PessoaNaoEncontradaException.java`

```java
package br.com.achernar.identity.pessoa.exception;

public class PessoaNaoEncontradaException extends RuntimeException {
    public PessoaNaoEncontradaException(Long id) {
        super("Pessoa não encontrada: " + id);
    }
}
```

Arquivo: `shared/exception/ApiExceptionHandler.java`

```java
package br.com.achernar.identity.shared.exception;

import br.com.achernar.identity.pessoa.exception.PessoaNaoEncontradaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiExceptionHandler {
    @ExceptionHandler(PessoaNaoEncontradaException.class)
    public ProblemDetail tratarPessoaNaoEncontrada(PessoaNaoEncontradaException ex) {
        ProblemDetail erro = ProblemDetail.forStatusAndDetail(
            HttpStatus.NOT_FOUND, "Pessoa não encontrada.");
        erro.setTitle("Recurso não encontrado");
        erro.setProperty("codigo", "PESSOA_NAO_ENCONTRADA");
        return erro;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail tratarValidacao(MethodArgumentNotValidException ex) {
        ProblemDetail erro = ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Revise os campos informados.");
        erro.setTitle("Dados inválidos");
        erro.setProperty("codigo", "DADOS_INVALIDOS");
        return erro;
    }
}
```

Expandir conforme erros reais do contrato. Logs sem sensíveis.

## Configuração e Liquibase

```yaml
spring:
  jpa:
    open-in-view: false
    hibernate:
      ddl-auto: validate
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
```

Arquivo ilustrativo: `src/main/resources/db/changelog/db.changelog-master.yaml`

```yaml
databaseChangeLog:
  - changeSet:
      id: 001-criar-pessoa
      author: achernar
      changes:
        - createTable:
            tableName: pessoa
            columns:
              - column:
                  name: id
                  type: bigint
                  autoIncrement: true
                  constraints:
                    primaryKey: true
                    nullable: false
              - column:
                  name: nome
                  type: varchar(120)
                  constraints:
                    nullable: false
```

Preservar convenção Liquibase; sem alterar changesets aplicados.

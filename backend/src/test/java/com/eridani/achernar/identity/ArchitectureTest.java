package com.eridani.achernar.identity;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/** Regras de arquitetura (ArchUnit): execute com {@code mvn test}. */
@AnalyzeClasses(packages = "com.eridani.achernar.identity", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureTest {

    @ArchTest
    static final ArchRule semJpa = noClasses().should().dependOnClassesThat()
            .resideInAnyPackage("jakarta.persistence..", "org.hibernate..", "org.springframework.data.jpa..")
            .because("a persistência é Spring JDBC explícito (decisão de implementação)");

    @ArchTest
    static final ArchRule controllersNaoAcessamJdbcDiretamente = noClasses().that().haveSimpleNameEndingWith("Controller")
            .should().dependOnClassesThat().haveFullyQualifiedName("org.springframework.jdbc.core.JdbcTemplate")
            .because("controllers delegam a serviços; SQL fica em repositórios/serviços");

    @ArchTest
    static final ArchRule repositoriosNaoDependemDeControllers = noClasses().that().haveSimpleNameEndingWith("Repository")
            .should().dependOnClassesThat().haveSimpleNameEndingWith("Controller");

    @ArchTest
    static final ArchRule semAlgoritmosFracesDeHash = noClasses().should().dependOnClassesThat()
            .haveFullyQualifiedName("org.apache.commons.codec.digest.DigestUtils");
}

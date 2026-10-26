# communs-arch

Regras de arquitetura testáveis para projetos Java, recriadas a partir do [ArchUnit](https://www.archunit.org/) com alterações para necessidades internas.

Requisitos: Java 21 e Maven 3.9+.

```bash
mvn verify
```

## Módulos

| Módulo | Responsabilidade |
| --- | --- |
| `communs-arch-core` | Importador de bytecode (ASM) e modelo de classes, membros, dependências e anotações |
| `communs-arch-lang` | DSL fluente `classes().that()...should()...` e avaliação de regras |
| `communs-arch-library` | Regras padrão e o catálogo `RegrasRegistry` |
| `communs-arch-junit` | Integração com JUnit 6 (`@AnalisarClasses`, `@RegraArch`) |
| `communs-arch-runner` | CLI que roda o catálogo num projeto e gera relatório JSON |

Cada módulo só depende dos que estão acima dele na tabela.

## Importando classes

```java
JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.exemplo.pedidos");

JavaClass service = classes.get("com.exemplo.pedidos.PedidoService").orElseThrow();
service.dependencies().forEach(d -> System.out.println(d.describe()));
// com.exemplo.pedidos.PedidoService -> com.exemplo.pedidos.PedidoRepository [METHOD_CALL] (linha 18)
```

O importador também lê pastas de classes e JARs (`importPaths`) ou classes avulsas (`importClasses`).

## Escrevendo regras

```java
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.classes;
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;

noClasses().that().resideInAPackage("..controller..")
        .should().accessClassesThat().resideInAPackage("..repository..")
        .because("controllers falam só com services")
        .check(classes);

classes().that().areAnnotatedWith(Service.class)
        .should().haveSimpleNameEndingWith("Service")
        .andShould().resideInAPackage("..service..")
        .check(classes);
```

Quando a regra é violada, `check` lança `ArchRuleViolationError` com uma linha por violação:

```
Architecture Violation - Rule 'no classes that reside in a package '..controller..' should access classes that reside in a package '..repository..', because controllers falam só com services' was violated (1 times):
Class <com.exemplo.controller.RelatorioController> accesses <com.exemplo.repository.PedidoRepository> [METHOD_CALL] in (RelatorioController.java:13)
```

Padrões de pacote seguem o ArchUnit: `..` é qualquer quantidade de pacotes e `*` é parte de um nome de pacote. Predicados e condições próprios entram com `that(DescribedPredicate)` e `should(ArchCondition)`. Regras alternativas (internas) e a integração com chat entram em fases posteriores.

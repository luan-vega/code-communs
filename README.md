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
| `communs-arch-qualidade` | Regras `QUALIDADE-xxx` que validam a configuração do Sonar, da cobertura e dos testes de mutação |
| `communs-arch-runner` | CLI que roda o catálogo num projeto e gera relatório JSON |
| `communs-qualidade-parent` | Pom pai com JaCoCo, PIT e Sonar configurados, para herdar nos projetos |

Cada módulo de código só depende dos que estão acima dele na tabela. O build da própria lib herda do `communs-qualidade-parent`.

## Importando classes

```java
JavaClasses classes = new ClassFileImporter()
        .withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS)
        .importPackages("com.exemplo.pedidos");

JavaClass service = classes.get("com.exemplo.pedidos.PedidoService").orElseThrow();
service.dependencies().forEach(d -> System.out.println(d.describe()));
// com.exemplo.pedidos.PedidoService -> com.exemplo.pedidos.PedidoRepository.buscar [METHOD_CALL] (linha 18)
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
Class <com.exemplo.controller.RelatorioController> accesses <com.exemplo.repository.PedidoRepository.buscar> [METHOD_CALL] in (RelatorioController.java:13)
```

Padrões de pacote seguem o ArchUnit: `..` é qualquer quantidade de pacotes e `*` é parte de um nome de pacote. Predicados e condições próprios entram com `that(DescribedPredicate)` e `should(ArchCondition)`. Regras alternativas (internas) e a integração com chat entram em fases posteriores.

## Regras padrão

O módulo `communs-arch-library` traz um catálogo de Regras com id estável. A documentação de cada uma (o que verifica, por quê, exemplo errado e certo) fica em [`communs-arch-library/src/main/resources/regras`](communs-arch-library/src/main/resources/regras).

| Id | Regra | Severidade |
| --- | --- | --- |
| CAMADAS-001 | Camadas seguem controller → service → repository | Erro |
| CAMADAS-002 | Domínio não depende de Spring nem de infraestrutura | Erro |
| CICLOS-001 | Sem ciclos entre pacotes de primeiro nível | Erro |
| NOMES-001 | Controllers REST terminam em Controller | Aviso |
| NOMES-002 | Services terminam em Service | Aviso |
| NOMES-003 | Interfaces de repositório terminam em Repository | Aviso |
| SPRING-001 | Sem injeção por campo | Erro |
| SPRING-002 | Entidades JPA não saem pelo controller | Aviso |
| CODIGO-001 | Sem System.out, System.err nem printStackTrace | Aviso |
| CODIGO-002 | Exceções de negócio estendem a exceção base | Aviso |

```java
ConfiguracaoRegras config = ConfiguracaoRegras.padrao().toBuilder()
        .pacoteRaiz("com.exemplo.pedidos")
        .excecaoBase("com.exemplo.pedidos.domain.ErroDeNegocio")
        .desligar("NOMES-003")
        .build();

List<ResultadoRegra> resultados = RegrasRegistry.padrao().avaliar(classes, config);
```

As Regras comparam anotações e tipos do Spring e do JPA pelo nome, então a lib não depende desses frameworks. Regras alternativas entram implementando `ProvedorDeRegras` e registrando a classe em `META-INF/services/io.github.luanvega.communs.arch.library.ProvedorDeRegras`.

## Configuração com `regras.yaml`

A mesma configuração pode vir de um arquivo, lido pela integração com JUnit e pela CLI:

```yaml
pacoteRaiz: com.exemplo.pedidos
excecaoBase: com.exemplo.pedidos.domain.ErroDeNegocio
camadas:
  controller: "..controller.."
  service: "..service.."
  repository: "..repository.."
  dominio: "..domain.."
  infra: ["..infra..", "..adapters.."]
desligar: [NOMES-003]
falharEmAvisos: false
```

Todas as chaves são opcionais; chaves desconhecidas dão erro, para um erro de digitação não desligar nada em silêncio. Em código: `LeitorDeConfiguracao.ler(Path.of("regras.yaml"))`.

## Testes com JUnit 6

Adicione `communs-arch-junit` com escopo `test`. A lib registra uma engine própria na JUnit Platform (`communs-arch`), que roda junto com o Jupiter no Surefire:

```java
@AnalisarClasses(pacotesDe = PedidosApplication.class, regrasPadrao = true)
class ArquiteturaTest {

    @RegraArch
    static final ArchRule semControllerNoRepositorio = noClasses().that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..");

    @RegraArch
    void regraEmCodigo(JavaClasses classes) {
        // qualquer verificação; uma exceção falha o teste
    }
}
```

- As classes são importadas uma vez por conjunto de pacotes e reaproveitadas entre as classes de teste.
- `regrasPadrao = true` vira um teste por Regra do catálogo (`CAMADAS-001`, `SPRING-001`...), configuradas pelo `regras.yaml` do classpath de teste (ou pelo arquivo em `configuracao`).
- Violação de Regra de AVISO aparece como teste abortado, não como falha, a menos que `falharEmAvisos: true`. Regra desligada ou sem configuração aparece abortada com o motivo.
- `incluirTestes = true` também analisa as classes de teste.

## CLI e relatório JSON

`communs-arch-runner` gera um JAR executável (`communs-arch-runner-<versão>-cli.jar`) para rodar o catálogo sem escrever testes, por exemplo no CI:

```bash
java -jar communs-arch-runner-0.1.0-SNAPSHOT-cli.jar --classes target/classes --config regras.yaml --json relatorio.json
```

| Opção | |
| --- | --- |
| `--classes <pasta\|jar>` | classes compiladas a analisar (pode repetir) |
| `--qualidade <pasta>` | verifica o `pom.xml` e o `sonar-project.properties` da pasta com as regras `QUALIDADE-xxx` |
| `--config <arquivo>` | `regras.yaml` (padrão: `./regras.yaml`, se existir) |
| `--json <arquivo\|->` | grava o relatório JSON no arquivo ou, com `-`, na saída padrão |
| `--falhar-em-avisos` | violações de AVISO também falham |
| `--incluir-testes` | analisa também classes de teste |

Saída 0 quando nada falhou, 1 quando alguma Regra falhou e 2 em erro de uso ou de configuração. O JSON traz um `resumo` (regras aprovadas, violadas, ignoradas, total de violações, `falhou`) e, por Regra, `id`, `severidade`, `status` (`APROVADA`, `VIOLADA` ou `IGNORADA`), `falha`, `motivo` e as `violacoes` com `mensagem`, `classe` e `linha`. É o formato que a integração com chat vai consumir. Em Java, o mesmo relatório sai de `new Runner().executar(caminhos, configuracao, incluirTestes)`.

## Qualidade: testes de mutação e Sonar

### Pom pai

Projetos que podem trocar de parent herdam do `communs-qualidade-parent` e ganham, sem configurar nada:

- JaCoCo medindo a cobertura e gerando o `jacoco.xml` que o Sonar lê (`mvn verify`);
- PIT com o `pitest-junit5-plugin`, que roda testes do JUnit 5 e do JUnit 6, no perfil `mutacao` (`mvn -Pmutacao verify`), falhando abaixo de `qualidade.mutacao.minimo` (padrão 60%);
- versão do `sonar-maven-plugin` e `sonar.qualitygate.wait=true`.

```xml
<parent>
    <groupId>io.github.luanvega.communs</groupId>
    <artifactId>communs-qualidade-parent</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</parent>
```

Projetos com `spring-boot-starter-parent` mantêm o parent e copiam os blocos de JaCoCo e PIT do [`communs-qualidade-parent/pom.xml`](communs-qualidade-parent/pom.xml). O relatório do PIT fica em `target/pit-reports/index.html`; no CI deste repositório ele é publicado como artefato do job `mutacao`.

### Regras QUALIDADE-xxx

| Id | Regra | Severidade |
| --- | --- | --- |
| QUALIDADE-001 | Sem credenciais do Sonar em arquivos | Erro |
| QUALIDADE-002 | Cobertura do JaCoCo chega ao Sonar | Erro |
| QUALIDADE-003 | Exclusões não escondem o código de produção | Erro |
| QUALIDADE-004 | Cobertura não exclui camadas com regra de negócio | Aviso |
| QUALIDADE-005 | Pipeline espera o Quality Gate | Aviso |
| QUALIDADE-006 | Testes de mutação configurados | Aviso |
| QUALIDADE-007 | SonarCloud com organização | Erro |

A documentação de cada uma fica em [`communs-arch-qualidade/src/main/resources/qualidade`](communs-arch-qualidade/src/main/resources/qualidade). Elas leem só os arquivos do projeto (o `pom.xml` da pasta e o `sonar-project.properties`), sem rodar o Maven. Pela CLI: `--qualidade .`; num teste:

```java
@Test
void configuracaoDeQualidade() {
    new VerificadorDeQualidade().verificar(Path.of(".")).exigirSemFalhas(false);
}
```

Os ids entram no `desligar` do `regras.yaml` como os das Regras de arquitetura.

### Skill do Claude

[`.claude/skills/qualidade-java`](.claude/skills/qualidade-java/SKILL.md) ensina o Claude Code a validar a configuração com essas regras, corrigir o `pom.xml`, rodar o PIT e explicar e matar os mutantes sobreviventes escrevendo testes. Ela vale dentro deste repositório; para usar em outros projetos, copie a pasta para `.claude/skills/` do projeto ou para `~/.claude/skills/`.

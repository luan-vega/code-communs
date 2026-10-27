---
name: qualidade-java
description: Roda e corrige testes de mutação (PIT) e valida a configuração do SonarQube/JaCoCo de um projeto Java/Maven com as regras QUALIDADE-xxx do code-communs. Use quando pedirem teste de mutação, mutantes sobreviventes, cobertura no Sonar, Quality Gate ou ajuste de sonar.* no pom.
---

# Qualidade em projetos Java (PIT + Sonar)

Esta skill faz três coisas num projeto Maven: valida a configuração do Sonar e da cobertura, roda os testes de mutação e explica e corrige o que encontrar. Responda em português.

## 1. Entender o projeto

- Leia o `pom.xml` da raiz (e dos módulos, se for multi-módulo) e o `sonar-project.properties`, se existir.
- Anote o `<parent>`: `communs-qualidade-parent`, `spring-boot-starter-parent` ou outro. Isso decide como corrigir no passo 3.
- Anote a versão do Java (`maven.compiler.release` ou `java.version`) e do JUnit (5 ou 6; os dois funcionam com o PIT pelo `pitest-junit5-plugin`).

## 2. Validar a configuração (regras QUALIDADE-xxx)

Se a CLI do code-communs estiver disponível (variável `COMMUNS_ARCH_CLI` com o caminho do `communs-arch-runner-<versão>-cli.jar`, ou o JAR em `communs-arch-runner/target/` deste repositório), rode:

```bash
java -jar "$COMMUNS_ARCH_CLI" --qualidade . --json -
```

O JSON traz `qualidade.regras[]` com `id`, `severidade`, `status` e `violacoes[].mensagem`. Saída 1 quer dizer que alguma regra de erro falhou.

Sem a CLI, verifique as mesmas regras lendo os arquivos. A explicação completa de cada uma (por quê, exemplo errado e certo) está em `communs-arch-qualidade/src/main/resources/qualidade/<ID>.md` no repositório code-communs:

| Id | Severidade | O que verificar |
| --- | --- | --- |
| QUALIDADE-001 | erro | `sonar.token`, `sonar.login`, `sonar.password` sem valor fixo (só `${env.X}`) |
| QUALIDADE-002 | erro | `jacoco-maven-plugin` com os goals `prepare-agent` e `report` (ou `report-aggregate`) |
| QUALIDADE-003 | erro | `sonar.exclusions`/`sonar.coverage.exclusions` sem `**`, `**/*`, `**/*.java`, `src/main/java/**` |
| QUALIDADE-004 | aviso | `sonar.coverage.exclusions` sem camadas com lógica (`service`, `domain`, `controller`, `usecase`, `application`) |
| QUALIDADE-005 | aviso | `sonar.qualitygate.wait=true` |
| QUALIDADE-006 | aviso | `pitest-maven` com a dependência `pitest-junit5-plugin` e `mutationThreshold` |
| QUALIDADE-007 | erro | com `sonar.host.url` no `sonarcloud.io`, `sonar.organization` preenchido |

Projetos que herdam do `communs-qualidade-parent` já passam em 002, 005 e 006.

## 3. Corrigir a configuração

- **Sem parent, ou com parent próprio que pode mudar:** proponha herdar do `io.github.luanvega.communs:communs-qualidade-parent`. Ele traz JaCoCo, PIT (perfil `mutacao`), versão do plugin do Sonar e `sonar.qualitygate.wait`.
- **Com `spring-boot-starter-parent`:** não troque o parent. Copie para o `pom.xml` do projeto os blocos de JaCoCo e PIT do `communs-qualidade-parent/pom.xml` (plugins, `pluginManagement` e o perfil `mutacao`), com as versões como propriedades.
- **Credenciais (001):** remova o valor do arquivo, diga ao usuário para revogar o token exposto e use `SONAR_TOKEN` como segredo do CI. Nunca escreva um token em arquivo nem o repita na resposta.
- **Exclusões (003/004):** não apague exclusões sem perguntar. Mostre o padrão problemático, proponha a lista só com o que não tem lógica (DTOs, configuração, classe `main`) e explique a troca.
- Se o Surefire tiver `<argLine>` próprio, troque por `@{argLine} ...` para não perder o agente do JaCoCo.
- Depois de mudar, rode `mvn -B verify` e a validação do passo 2 de novo.

## 4. Rodar os testes de mutação

```bash
# com o communs-qualidade-parent ou o perfil copiado
mvn -B -Pmutacao verify
# sem perfil (o pitest-maven com o pitest-junit5-plugin precisa estar no pom)
mvn -B test-compile org.pitest:pitest-maven:mutationCoverage
# só uma parte, para iterar rápido
mvn -B test-compile org.pitest:pitest-maven:mutationCoverage \
    -DtargetClasses='com.exemplo.pedidos.service.*' -DtargetTests='com.exemplo.pedidos.service.*'
```

O relatório fica em `target/pit-reports/` (`index.html` para pessoas, `mutations.xml` para ler aqui). Se o perfil falhar por `mutationThreshold`, o build mostra o percentual atingido e o mínimo.

## 5. Explicar e corrigir os mutantes

1. Leia `target/pit-reports/mutations.xml` e separe os `<mutation status="SURVIVED">` e `NO_COVERAGE`, agrupados por classe e método (`mutatedClass`, `mutatedMethod`, `lineNumber`, `description`).
2. Explique cada grupo em linguagem simples, por exemplo: "na linha 42 do `PedidoService.calcularFrete`, trocar `>` por `>=` não quebrou nenhum teste: falta um teste com o valor exatamente no limite".
3. Corrija escrevendo ou ajustando **testes**, não o código de produção:
   - limites (`>` e `>=`): teste o valor exato do limite e um de cada lado;
   - retorno trocado por vazio, `null`, `0` ou `false`: faça o teste verificar o valor retornado, não só que não lançou exceção;
   - chamada removida (`VoidMethodCallMutator`): verifique o efeito da chamada (estado, mock com `verify`, evento publicado);
   - condição negada: cubra os dois ramos.
4. Mutante equivalente (a mudança não altera o comportamento, ex.: log, `toString`, otimização) não se mata com teste: explique por que e deixe como está.
5. Se o código mutado não é usado por ninguém, aponte como código morto e pergunte antes de apagar.
6. Rode de novo só a classe alterada (com `-DtargetClasses`) até os sobreviventes relevantes morrerem, depois o build completo.

## 6. Resumo final

Termine com uma tabela curta: regras QUALIDADE com problema e o que foi feito; força de mutação antes e depois por módulo; mutantes que ficaram e por quê. Não suba o `mutationThreshold` sem combinar com o usuário.

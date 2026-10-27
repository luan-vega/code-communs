# QUALIDADE-002 Cobertura do JaCoCo chega ao Sonar

**Severidade:** erro

## O que verifica
O `jacoco-maven-plugin` está no `pom.xml` com os goals `prepare-agent` (mede a cobertura enquanto os testes rodam) e `report` ou `report-aggregate` (gera o `jacoco.xml`). Projetos que herdam do `communs-qualidade-parent` já têm isso.

## Por quê
O Sonar não roda os testes: ele só lê o `target/site/jacoco/jacoco.xml`. Sem o relatório, a cobertura aparece como 0% e o Quality Gate de cobertura reprova ou fica sem sentido.

## Errado
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
    </executions>
</plugin>
```

## Certo
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <execution>
            <goals><goal>prepare-agent</goal></goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
        </execution>
    </executions>
</plugin>
```
Se o Surefire tiver `argLine` próprio, use `@{argLine} -Xmx...` para não apagar o agente do JaCoCo.

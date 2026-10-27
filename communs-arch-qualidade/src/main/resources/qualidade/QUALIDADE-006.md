# QUALIDADE-006 Testes de mutação configurados

**Severidade:** aviso

## O que verifica
O `pitest-maven` está no `pom.xml`, com a dependência `pitest-junit5-plugin` (que também roda testes do JUnit 6) e um `mutationThreshold`. Projetos que herdam do `communs-qualidade-parent` já têm isso.

## Por quê
Cobertura diz que a linha rodou, não que algum teste percebe quando ela muda. O PIT altera o código (troca `>` por `>=`, apaga chamadas, inverte condições) e conta quantas mudanças os testes pegam. Sem o plugin do JUnit 5 o PIT não acha os testes; sem o limite, a força dos testes pode cair sem ninguém ver.

## Errado
```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.30.1</version>
</plugin>
```

## Certo
```xml
<plugin>
    <groupId>org.pitest</groupId>
    <artifactId>pitest-maven</artifactId>
    <version>1.30.1</version>
    <dependencies>
        <dependency>
            <groupId>org.pitest</groupId>
            <artifactId>pitest-junit5-plugin</artifactId>
            <version>1.2.3</version>
        </dependency>
    </dependencies>
    <configuration>
        <mutationThreshold>60</mutationThreshold>
    </configuration>
</plugin>
```
Rode com `mvn org.pitest:pitest-maven:mutationCoverage` e veja o relatório em `target/pit-reports/index.html`.

# QUALIDADE-005 Pipeline espera o Quality Gate

**Severidade:** aviso

## O que verifica
`sonar.qualitygate.wait` é `true` no `pom.xml` ou no `sonar-project.properties`. Projetos que herdam do `communs-qualidade-parent` já têm isso.

## Por quê
Sem essa propriedade o scanner só envia a análise e termina com sucesso; o Quality Gate é calculado depois, no servidor, e um código reprovado chega à branch principal.

## Errado
```xml
<properties>
    <sonar.projectKey>pedidos</sonar.projectKey>
</properties>
```

## Certo
```xml
<properties>
    <sonar.projectKey>pedidos</sonar.projectKey>
    <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
</properties>
```

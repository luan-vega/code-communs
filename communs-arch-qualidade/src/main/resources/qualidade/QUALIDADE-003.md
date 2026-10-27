# QUALIDADE-003 Exclusões não escondem o código de produção

**Severidade:** erro

## O que verifica
`sonar.exclusions` e `sonar.coverage.exclusions` não têm padrões que pegam todo o código, como `**`, `**/*`, `**/*.java` ou `src/main/java/**`.

## Por quê
Com um desses padrões o Sonar não analisa nada (ou não mede cobertura de nada) e o Quality Gate passa sempre. É um jeito comum de "fazer o pipeline passar" que esconde o problema.

## Errado
```properties
sonar.coverage.exclusions=**/*.java
```

## Certo
Exclua só o que não tem lógica e diga por quê num comentário:
```properties
# DTOs e configuração do Spring, sem regra de negócio
sonar.coverage.exclusions=**/dto/**,**/config/**,**/*Application.java
```

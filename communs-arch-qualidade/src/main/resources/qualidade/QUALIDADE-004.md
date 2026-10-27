# QUALIDADE-004 Cobertura não exclui camadas com regra de negócio

**Severidade:** aviso

## O que verifica
`sonar.coverage.exclusions` não tem padrões com `service`, `domain`, `dominio`, `controller`, `usecase` ou `application`.

## Por quê
É nessas camadas que mora a regra de negócio, justamente o código que mais precisa de teste. Excluir da cobertura deixa o número bonito e o risco escondido.

## Errado
```properties
sonar.coverage.exclusions=**/service/**,**/dto/**
```

## Certo
```properties
sonar.coverage.exclusions=**/dto/**
```
Se uma classe específica não tem como ser testada, exclua só ela, com o motivo num comentário.

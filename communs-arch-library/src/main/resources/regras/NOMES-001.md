# NOMES-001 Controllers REST terminam em Controller

**Severidade:** aviso

## O que verifica
Classes anotadas com `@RestController` têm nome simples terminado em `Controller`.

## Por quê
Um sufixo fixo deixa claro, só pelo nome, onde ficam os endpoints, e facilita buscas e outras regras por nome.

## Errado
```java
@RestController
class PedidoApi {
}
```

## Certo
```java
@RestController
class PedidoController {
}
```

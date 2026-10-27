# NOMES-002 Services terminam em Service

**Severidade:** aviso

## O que verifica
Classes anotadas com `@Service` têm nome simples terminado em `Service`.

## Por quê
O sufixo identifica a camada de aplicação sem abrir o arquivo e mantém o padrão entre projetos.

## Errado
```java
@Service
class Pedidos {
}
```

## Certo
```java
@Service
class PedidoService {
}
```

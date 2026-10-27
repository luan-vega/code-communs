# SPRING-002 Entidades JPA não saem pelo controller

**Severidade:** aviso

## O que verifica
Métodos não privados de controllers (classes na camada controller ou anotadas com `@RestController`/`@Controller`)
não recebem nem devolvem classes anotadas com `@Entity` (`jakarta.persistence` ou `javax.persistence`).

Limitação atual: tipos genéricos, como `List<Pedido>`, ainda não são verificados.

## Por quê
Expor a entidade acopla o contrato da API ao modelo do banco, vaza campos internos e pode disparar
carregamento preguiçoso fora da transação.

## Errado
```java
@GetMapping("/{id}")
Pedido get(@PathVariable String id) {
    return service.buscar(id);
}
```

## Certo
```java
@GetMapping("/{id}")
PedidoDto get(@PathVariable String id) {
    return PedidoDto.de(service.buscar(id));
}
```

## Configuração
`pacoteController` (`..controller..`).

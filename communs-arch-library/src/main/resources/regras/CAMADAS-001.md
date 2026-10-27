# CAMADAS-001 Camadas seguem controller → service → repository

**Severidade:** erro

## O que verifica
- Classes da camada controller não acessam (chamam métodos, construtores ou campos) classes da camada repository.
- Classes da camada service não acessam classes da camada controller.
- Classes da camada repository não acessam classes das camadas service nem controller.

## Por quê
Cada camada tem uma responsabilidade. Quando o controller fala direto com o repositório, regras de negócio
e transações ficam espalhadas, e a mesma lógica acaba duplicada em vários endpoints.

## Errado
```java
@RestController
class PedidoController {
    private final PedidoRepository repository;

    Pedido get(String id) {
        return repository.findById(id); // controller acessando o repositório
    }
}
```

## Certo
```java
@RestController
class PedidoController {
    private final PedidoService service;

    PedidoDto get(String id) {
        return PedidoDto.de(service.buscar(id));
    }
}
```

## Configuração
Padrões de pacote: `pacoteController` (`..controller..`), `pacoteService` (`..service..`) e `pacoteRepository` (`..repository..`).

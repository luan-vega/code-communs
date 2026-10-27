# SPRING-001 Sem injeção por campo

**Severidade:** erro

## O que verifica
Nenhum campo é anotado com `@Autowired`, `jakarta.inject.Inject` ou `javax.inject.Inject`.

## Por quê
Injeção por campo esconde dependências, impede campos `final` e obriga a usar reflexão ou o contexto do Spring nos testes.
Com construtor, as dependências ficam explícitas e o objeto nasce completo.

## Errado
```java
@Service
class PedidoService {
    @Autowired
    private PedidoRepository repository;
}
```

## Certo
```java
@Service
class PedidoService {
    private final PedidoRepository repository;

    PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }
}
```

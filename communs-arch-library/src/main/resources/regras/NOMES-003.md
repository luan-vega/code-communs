# NOMES-003 Interfaces de repositório terminam em Repository

**Severidade:** aviso

## O que verifica
Interfaces que estendem `org.springframework.data.repository.Repository` (direta ou indiretamente, como
`JpaRepository`) ou anotadas com `@Repository` têm nome simples terminado em `Repository`.

## Por quê
Mantém o vocabulário do Spring Data e evita nomes como `Dao`, `Store` ou `Gateway` para a mesma coisa.

## Errado
```java
interface PedidoDao extends JpaRepository<Pedido, String> {
}
```

## Certo
```java
interface PedidoRepository extends JpaRepository<Pedido, String> {
}
```

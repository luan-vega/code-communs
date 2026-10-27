# CODIGO-001 Sem System.out, System.err nem printStackTrace

**Severidade:** aviso

## O que verifica
Nenhuma classe usa `System.out`, `System.err` nem chama `printStackTrace()` de exceções.

## Por quê
Essas saídas ignoram o nível, o formato e o destino configurados no log da aplicação, e somem em produção.

## Errado
```java
catch (IOException e) {
    e.printStackTrace();
}
```

## Certo
```java
private static final Logger log = LoggerFactory.getLogger(PedidoService.class);

catch (IOException e) {
    log.error("Falha ao ler o pedido {}", id, e);
}
```

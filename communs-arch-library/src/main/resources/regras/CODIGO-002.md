# CODIGO-002 Exceções de negócio estendem a exceção base

**Severidade:** aviso

## O que verifica
Exceções no pacote de domínio herdam da exceção base do projeto.

## Por quê
Com uma base comum, o tratamento global (ex.: `@ControllerAdvice`) responde a todos os erros de negócio
do mesmo jeito, e nenhuma exceção nova escapa como erro 500.

## Errado
```java
public class PedidoInvalido extends RuntimeException {
}
```

## Certo
```java
public class PedidoInvalido extends ErroDeNegocio {
}
```

## Configuração
`excecaoBase` com o nome completo da classe base. Sem essa configuração, a regra é ignorada.
`pacoteDominio` (`..domain..`).

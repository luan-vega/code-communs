# CICLOS-001 Sem ciclos entre pacotes de primeiro nível

**Severidade:** erro

## O que verifica
Agrupa as classes pelos subpacotes diretos do pacote raiz (ex.: `com.app.pedido`, `com.app.estoque`) e procura
ciclos de dependência entre esses grupos. Cada ciclo vira uma violação, com um exemplo de dependência por aresta.

## Por quê
Pacotes em ciclo não podem ser entendidos, testados nem extraídos separadamente. Ciclos costumam começar pequenos
e crescer até travar qualquer modularização.

## Errado
```java
package com.app.pedido;
class Pedido { Estoque estoque; }

package com.app.estoque;
class Estoque { Pedido ultimoPedido; } // estoque -> pedido -> estoque
```

## Certo
```java
package com.app.pedido;
class Pedido { Estoque estoque; }

package com.app.estoque;
class Estoque { String ultimoPedidoId; } // só uma direção
```

## Configuração
`pacoteRaiz`. Quando não informado, usa o maior pacote comum a todas as classes importadas.

# CAMADAS-002 Domínio não depende de Spring nem de infraestrutura

**Severidade:** erro

## O que verifica
Classes do domínio não dependem de `org.springframework..`, nem das camadas controller, repository ou infraestrutura.
Qualquer tipo de dependência conta: anotações, campos, parâmetros e chamadas.

## Por quê
O domínio guarda as regras de negócio. Se ele conhece o framework ou o banco, fica difícil testar sem subir
o contexto e qualquer troca de tecnologia vaza para o núcleo do sistema.

## Errado
```java
@Component // anotação do Spring no domínio
public class Pedido {
}
```

## Certo
```java
public class Pedido {
}

@Configuration
class PedidoConfig {
    // o Spring monta o domínio de fora para dentro
}
```

## Configuração
`pacoteDominio` (`..domain..`), `pacotesInfra` (`..infra..` e `..infrastructure..`), além dos pacotes de controller e repository.

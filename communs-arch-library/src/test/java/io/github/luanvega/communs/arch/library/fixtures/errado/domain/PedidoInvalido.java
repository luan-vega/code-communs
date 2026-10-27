package io.github.luanvega.communs.arch.library.fixtures.errado.domain;

// CODIGO-002: estende RuntimeException em vez de ErroDeNegocio.
public class PedidoInvalido extends RuntimeException {

    public PedidoInvalido(String mensagem) {
        super(mensagem);
    }
}

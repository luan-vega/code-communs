package io.github.luanvega.communs.arch.library.fixtures.certo.domain;

public class PedidoNaoEncontrado extends ErroDeNegocio {

    public PedidoNaoEncontrado(String id) {
        super("Pedido não encontrado: " + id);
    }
}

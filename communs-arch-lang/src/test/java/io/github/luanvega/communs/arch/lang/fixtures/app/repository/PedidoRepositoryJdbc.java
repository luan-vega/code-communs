package io.github.luanvega.communs.arch.lang.fixtures.app.repository;

import io.github.luanvega.communs.arch.lang.fixtures.app.domain.Pedido;

public class PedidoRepositoryJdbc implements PedidoRepository {

    @Override
    public Pedido buscar(String id) {
        return new Pedido(id);
    }
}

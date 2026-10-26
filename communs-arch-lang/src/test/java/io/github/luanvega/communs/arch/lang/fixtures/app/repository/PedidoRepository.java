package io.github.luanvega.communs.arch.lang.fixtures.app.repository;

import io.github.luanvega.communs.arch.lang.fixtures.app.domain.Pedido;

public interface PedidoRepository {

    Pedido buscar(String id);
}

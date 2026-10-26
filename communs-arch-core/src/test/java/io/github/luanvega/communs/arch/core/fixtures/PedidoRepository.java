package io.github.luanvega.communs.arch.core.fixtures;

import java.util.Optional;

public interface PedidoRepository {

    Optional<Pedido> buscar(String id);
}

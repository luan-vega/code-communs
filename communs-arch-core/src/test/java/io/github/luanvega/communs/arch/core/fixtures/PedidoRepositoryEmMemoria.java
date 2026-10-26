package io.github.luanvega.communs.arch.core.fixtures;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PedidoRepositoryEmMemoria implements PedidoRepository {

    private final Map<String, Pedido> pedidos = new HashMap<>();

    @Override
    public Optional<Pedido> buscar(String id) {
        return Optional.ofNullable(pedidos.get(id));
    }
}

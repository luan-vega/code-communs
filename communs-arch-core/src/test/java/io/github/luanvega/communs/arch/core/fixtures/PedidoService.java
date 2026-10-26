package io.github.luanvega.communs.arch.core.fixtures;

import java.math.BigDecimal;
import java.util.List;

@Marcador
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    @Marcador
    public Pedido criar(@Marcador String id) throws IllegalStateException {
        if (repository.buscar(id).isPresent()) {
            throw new IllegalStateException("Pedido já existe: " + id);
        }
        return new Pedido(id, BigDecimal.ZERO, Status.ABERTO);
    }

    public List<String> ids(List<Pedido> pedidos) {
        return pedidos.stream().map(Pedido::id).toList();
    }

    public boolean ehPedido(Object valor) {
        return valor instanceof Pedido;
    }
}

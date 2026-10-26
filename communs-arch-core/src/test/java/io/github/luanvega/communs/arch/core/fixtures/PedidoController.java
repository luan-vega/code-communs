package io.github.luanvega.communs.arch.core.fixtures;

public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    public Pedido post(String id) {
        return service.criar(id);
    }
}

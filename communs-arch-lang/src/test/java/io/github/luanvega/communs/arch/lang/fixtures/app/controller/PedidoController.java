package io.github.luanvega.communs.arch.lang.fixtures.app.controller;

import io.github.luanvega.communs.arch.lang.fixtures.app.domain.Pedido;
import io.github.luanvega.communs.arch.lang.fixtures.app.service.PedidoService;

public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    public Pedido get(String id) {
        return service.buscar(id);
    }
}

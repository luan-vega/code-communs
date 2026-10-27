package io.github.luanvega.communs.arch.junit.fixtures.certo.controller;

import io.github.luanvega.communs.arch.junit.fixtures.certo.service.PedidoService;

public class PedidoController {

    private final PedidoService service = new PedidoService();

    public String get(String id) {
        return service.buscar(id);
    }
}

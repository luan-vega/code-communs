package io.github.luanvega.communs.arch.lang.fixtures.app.controller;

import io.github.luanvega.communs.arch.lang.fixtures.app.Injetar;
import io.github.luanvega.communs.arch.lang.fixtures.app.domain.Pedido;
import io.github.luanvega.communs.arch.lang.fixtures.app.repository.PedidoRepository;

public class RelatorioController {

    @Injetar
    private PedidoRepository repository;

    public Pedido get(String id) {
        return repository.buscar(id);
    }
}

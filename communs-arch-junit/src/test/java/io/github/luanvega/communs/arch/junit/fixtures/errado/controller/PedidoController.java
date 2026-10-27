package io.github.luanvega.communs.arch.junit.fixtures.errado.controller;

import io.github.luanvega.communs.arch.junit.fixtures.errado.repository.PedidoRepository;

public class PedidoController {

    private final PedidoRepository repository = new PedidoRepository();

    public String get(String id) {
        System.out.println(id);
        return repository.buscar(id);
    }
}

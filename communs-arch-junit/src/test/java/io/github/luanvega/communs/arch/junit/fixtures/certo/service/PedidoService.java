package io.github.luanvega.communs.arch.junit.fixtures.certo.service;

import io.github.luanvega.communs.arch.junit.fixtures.certo.repository.PedidoRepository;

public class PedidoService {

    private final PedidoRepository repository = new PedidoRepository();

    public String buscar(String id) {
        return repository.buscar(id);
    }
}

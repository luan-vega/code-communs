package io.github.luanvega.communs.arch.lang.fixtures.app.service;

import io.github.luanvega.communs.arch.lang.fixtures.app.Servico;
import io.github.luanvega.communs.arch.lang.fixtures.app.domain.Pedido;
import io.github.luanvega.communs.arch.lang.fixtures.app.domain.PedidoInvalidoException;
import io.github.luanvega.communs.arch.lang.fixtures.app.repository.PedidoRepository;

@Servico
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public Pedido buscar(String id) {
        if (id == null) {
            throw new PedidoInvalidoException("id obrigatório");
        }
        return repository.buscar(id);
    }
}

package io.github.luanvega.communs.arch.library.fixtures.certo.service;

import io.github.luanvega.communs.arch.library.fixtures.certo.domain.Pedido;
import io.github.luanvega.communs.arch.library.fixtures.certo.domain.PedidoNaoEncontrado;
import io.github.luanvega.communs.arch.library.fixtures.certo.repository.PedidoRepository;
import org.springframework.stereotype.Service;

@Service
public class PedidoService {

    private final PedidoRepository repository;

    public PedidoService(PedidoRepository repository) {
        this.repository = repository;
    }

    public Pedido buscar(String id) {
        Pedido pedido = repository.findById(id);
        if (pedido == null) {
            throw new PedidoNaoEncontrado(id);
        }
        return pedido;
    }
}

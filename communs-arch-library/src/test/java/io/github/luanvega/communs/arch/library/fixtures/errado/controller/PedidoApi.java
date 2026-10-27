package io.github.luanvega.communs.arch.library.fixtures.errado.controller;

import io.github.luanvega.communs.arch.library.fixtures.errado.domain.Pedido;
import io.github.luanvega.communs.arch.library.fixtures.errado.repository.PedidoDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

// NOMES-001: não termina em Controller.
@RestController
public class PedidoApi {

    // SPRING-001: injeção por campo.
    @Autowired
    private PedidoDao dao;

    // SPRING-002: devolve a entidade. CAMADAS-001: acessa o repositório direto.
    public Pedido get(String id) {
        System.out.println("buscando " + id);
        return dao.findById(id);
    }
}

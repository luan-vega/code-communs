package io.github.luanvega.communs.arch.library.fixtures.certo.controller;

import io.github.luanvega.communs.arch.library.fixtures.certo.dto.PedidoDto;
import io.github.luanvega.communs.arch.library.fixtures.certo.service.PedidoService;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PedidoController {

    private final PedidoService service;

    public PedidoController(PedidoService service) {
        this.service = service;
    }

    public PedidoDto get(String id) {
        return new PedidoDto(service.buscar(id).id());
    }
}

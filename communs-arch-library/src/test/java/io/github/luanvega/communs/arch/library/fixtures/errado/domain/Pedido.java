package io.github.luanvega.communs.arch.library.fixtures.errado.domain;

import io.github.luanvega.communs.arch.library.fixtures.errado.service.Pedidos;
import jakarta.persistence.Entity;
import org.springframework.stereotype.Component;

// CAMADAS-002: depende de Spring. CICLOS-001: domain -> service -> domain.
@Entity
@Component
public class Pedido {

    private final String id;

    public Pedido(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public void recalcular(Pedidos pedidos) {
        pedidos.contar();
    }
}

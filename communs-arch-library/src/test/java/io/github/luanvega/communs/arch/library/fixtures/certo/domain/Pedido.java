package io.github.luanvega.communs.arch.library.fixtures.certo.domain;

import jakarta.persistence.Entity;

@Entity
public class Pedido {

    private final String id;

    public Pedido(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }
}

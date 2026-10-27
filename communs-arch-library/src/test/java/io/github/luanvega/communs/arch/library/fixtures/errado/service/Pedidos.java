package io.github.luanvega.communs.arch.library.fixtures.errado.service;

import io.github.luanvega.communs.arch.library.fixtures.errado.domain.Pedido;
import org.springframework.stereotype.Service;

// NOMES-002: não termina em Service.
@Service
public class Pedidos {

    public int contar() {
        try {
            return Integer.parseInt("1");
        } catch (NumberFormatException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public Pedido novo(String id) {
        return new Pedido(id);
    }
}

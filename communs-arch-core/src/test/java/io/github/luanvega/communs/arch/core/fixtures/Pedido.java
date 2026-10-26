package io.github.luanvega.communs.arch.core.fixtures;

import java.math.BigDecimal;

public record Pedido(String id, BigDecimal valor, Status status) {

    public static final class Item {
        private final String descricao;

        public Item(String descricao) {
            this.descricao = descricao;
        }

        public String descricao() {
            return descricao;
        }
    }
}

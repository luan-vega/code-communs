package io.github.luanvega.communs.arch.library.fixtures.errado.repository;

import io.github.luanvega.communs.arch.library.fixtures.errado.domain.Pedido;
import org.springframework.data.repository.Repository;

// NOMES-003: não termina em Repository.
public interface PedidoDao extends Repository<Pedido, String> {

    Pedido findById(String id);
}

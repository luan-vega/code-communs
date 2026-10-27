package io.github.luanvega.communs.arch.library.fixtures.certo.repository;

import io.github.luanvega.communs.arch.library.fixtures.certo.domain.Pedido;
import org.springframework.data.repository.Repository;

public interface PedidoRepository extends Repository<Pedido, String> {

    Pedido findById(String id);
}

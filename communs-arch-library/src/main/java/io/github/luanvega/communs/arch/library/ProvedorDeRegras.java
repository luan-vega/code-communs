package io.github.luanvega.communs.arch.library;

import java.util.List;

/**
 * Fornece Regras para o {@link RegrasRegistry}. Regras alternativas (internas) implementam esta interface
 * e se registram em {@code META-INF/services/io.github.luanvega.communs.arch.library.ProvedorDeRegras}.
 */
public interface ProvedorDeRegras {

    List<Regra> regras();
}

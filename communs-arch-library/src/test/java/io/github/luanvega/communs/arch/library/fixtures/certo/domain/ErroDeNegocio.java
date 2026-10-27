package io.github.luanvega.communs.arch.library.fixtures.certo.domain;

public class ErroDeNegocio extends RuntimeException {

    public ErroDeNegocio(String mensagem) {
        super(mensagem);
    }
}

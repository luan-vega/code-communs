package io.github.luanvega.communs.arch.library.fixtures.errado.domain;

public class ErroDeNegocio extends RuntimeException {

    public ErroDeNegocio(String mensagem) {
        super(mensagem);
    }
}

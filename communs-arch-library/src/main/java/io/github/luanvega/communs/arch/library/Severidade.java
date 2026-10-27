package io.github.luanvega.communs.arch.library;

/**
 * Peso de uma violação: {@link #ERRO} deve quebrar o build; {@link #AVISO} só é reportado.
 */
public enum Severidade {
    ERRO,
    AVISO
}

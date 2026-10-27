package io.github.luanvega.communs.arch.qualidade;

import io.github.luanvega.communs.arch.library.Severidade;
import java.util.List;
import java.util.Optional;

/**
 * Resultado de uma {@link RegraDeQualidade}: as violações encontradas ou, se a regra foi desligada, o motivo.
 */
public record ResultadoQualidade(RegraDeQualidade regra, List<String> violacoes, Optional<String> motivo) {

    public ResultadoQualidade {
        violacoes = List.copyOf(violacoes);
    }

    static ResultadoQualidade avaliada(RegraDeQualidade regra, List<String> violacoes) {
        return new ResultadoQualidade(regra, violacoes, Optional.empty());
    }

    static ResultadoQualidade ignorada(RegraDeQualidade regra, String motivo) {
        return new ResultadoQualidade(regra, List.of(), Optional.of(motivo));
    }

    public boolean foiIgnorada() {
        return motivo.isPresent();
    }

    public boolean temViolacoes() {
        return !violacoes.isEmpty();
    }

    /** {@code true} quando a regra tem violações que devem quebrar o build. */
    public boolean deveFalhar(boolean falharEmAvisos) {
        return temViolacoes() && (regra.severidade() == Severidade.ERRO || falharEmAvisos);
    }
}

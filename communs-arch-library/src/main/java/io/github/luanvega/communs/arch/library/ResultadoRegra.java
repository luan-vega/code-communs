package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.EvaluationResult;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Resultado de uma Regra do catálogo num projeto: avaliada (com ou sem violações) ou ignorada, com o motivo.
 */
public record ResultadoRegra(Regra regra, EvaluationResult avaliacao, String motivoIgnorada) {

    public ResultadoRegra {
        Objects.requireNonNull(regra, "regra");
        if ((avaliacao == null) == (motivoIgnorada == null)) {
            throw new IllegalArgumentException("Informe a avaliação ou o motivo de ter sido ignorada, não os dois");
        }
    }

    public static ResultadoRegra avaliada(Regra regra, EvaluationResult avaliacao) {
        return new ResultadoRegra(regra, avaliacao, null);
    }

    public static ResultadoRegra ignorada(Regra regra, String motivo) {
        return new ResultadoRegra(regra, null, motivo);
    }

    public boolean foiIgnorada() {
        return avaliacao == null;
    }

    public boolean temViolacoes() {
        return avaliacao != null && avaliacao.hasViolations();
    }

    public List<ConditionEvent> violacoes() {
        return avaliacao == null ? List.of() : avaliacao.violations();
    }

    public Optional<String> motivo() {
        return Optional.ofNullable(motivoIgnorada);
    }
}

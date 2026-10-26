package io.github.luanvega.communs.arch.lang;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;

/**
 * Regra de arquitetura avaliada sobre um conjunto de classes importadas.
 */
public interface ArchRule {

    String description();

    EvaluationResult evaluate(JavaClasses classes);

    /**
     * Avalia a regra e lança {@link ArchRuleViolationError} se houver violações.
     */
    default void check(JavaClasses classes) {
        EvaluationResult result = evaluate(classes);
        if (result.hasViolations()) {
            throw new ArchRuleViolationError(result);
        }
    }

    /**
     * Acrescenta a justificativa à descrição, ex.: "... because controllers não devem acessar o banco".
     */
    ArchRule because(String reason);

    /**
     * Por padrão, uma regra que não seleciona nenhuma classe falha, porque costuma indicar um filtro errado.
     * Use {@code allowEmptyShould(true)} quando isso for esperado.
     */
    ArchRule allowEmptyShould(boolean allow);

    /**
     * Substitui a descrição gerada pela DSL.
     */
    ArchRule as(String newDescription);
}

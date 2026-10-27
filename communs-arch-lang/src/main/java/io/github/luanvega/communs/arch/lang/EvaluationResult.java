package io.github.luanvega.communs.arch.lang;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resultado de avaliar uma regra: a descrição da regra e as violações encontradas.
 */
public record EvaluationResult(String ruleDescription, List<ConditionEvent> violations) {

    public EvaluationResult {
        violations = List.copyOf(violations);
    }

    public boolean hasViolations() {
        return !violations.isEmpty();
    }

    /**
     * Texto do relatório, no formato que aparece quando a regra falha num teste.
     */
    public String report() {
        if (!hasViolations()) {
            return "Rule '" + ruleDescription + "' was satisfied";
        }
        String details = violations.stream().map(ConditionEvent::message).collect(Collectors.joining(System.lineSeparator()));
        return "Architecture Violation - Rule '" + ruleDescription + "' was violated (" + violations.size() + " times):"
                + System.lineSeparator() + details;
    }
}

package io.github.luanvega.communs.arch.lang;

/**
 * Lançada por {@link ArchRule#check} quando a regra é violada. Estende {@link AssertionError}
 * para o teste falhar como uma asserção comum.
 */
public final class ArchRuleViolationError extends AssertionError {

    private final transient EvaluationResult result;

    public ArchRuleViolationError(EvaluationResult result) {
        super(result.report());
        this.result = result;
    }

    public EvaluationResult result() {
        return result;
    }
}

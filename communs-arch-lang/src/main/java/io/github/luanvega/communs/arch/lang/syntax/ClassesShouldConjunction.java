package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.EvaluationResult;

/**
 * Regra completa, que ainda aceita mais condições com {@code andShould()}/{@code orShould()}.
 */
public final class ClassesShouldConjunction implements ArchRule {

    private final ClassesRule rule;

    ClassesShouldConjunction(ClassesRule rule) {
        this.rule = rule;
    }

    public ClassesShould andShould() {
        return new ClassesShould(condition -> new ClassesShouldConjunction(rule.withCondition(rule.condition().and(condition))));
    }

    public ClassesShouldConjunction andShould(ArchCondition<? super JavaClass> condition) {
        return andShould().satisfy(condition);
    }

    public ClassesShould orShould() {
        return new ClassesShould(condition -> new ClassesShouldConjunction(rule.withCondition(rule.condition().or(condition))));
    }

    public ClassesShouldConjunction orShould(ArchCondition<? super JavaClass> condition) {
        return orShould().satisfy(condition);
    }

    @Override
    public String description() {
        return rule.description();
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        return rule.evaluate(classes);
    }

    @Override
    public ArchRule because(String reason) {
        return rule.because(reason);
    }

    @Override
    public ArchRule allowEmptyShould(boolean allow) {
        return rule.allowEmptyShould(allow);
    }

    @Override
    public ArchRule as(String newDescription) {
        return rule.as(newDescription);
    }

    @Override
    public String toString() {
        return description();
    }
}

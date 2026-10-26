package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.ConditionEvents;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;
import io.github.luanvega.communs.arch.lang.EvaluationResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Regra imutável montada pela DSL: quais classes ({@code predicate}), o que devem atender ({@code condition})
 * e se a condição é proibida ({@code negated}, em {@code noClasses()}).
 */
record ClassesRule(
        boolean negated,
        DescribedPredicate<JavaClass> predicate,
        ArchCondition<JavaClass> condition,
        String reason,
        boolean allowEmpty,
        String overriddenDescription) implements ArchRule {

    static ClassesRule of(boolean negated, DescribedPredicate<JavaClass> predicate, ArchCondition<JavaClass> condition) {
        return new ClassesRule(negated, predicate, condition, null, false, null);
    }

    ClassesRule withCondition(ArchCondition<JavaClass> newCondition) {
        return new ClassesRule(negated, predicate, newCondition, reason, allowEmpty, overriddenDescription);
    }

    @Override
    public String description() {
        String base = overriddenDescription != null ? overriddenDescription : generatedDescription();
        return reason != null ? base + ", because " + reason : base;
    }

    private String generatedDescription() {
        String subject = (negated ? "no classes" : "classes") + (predicate != null ? " that " + predicate.description() : "");
        return subject + " should " + condition.description();
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        JavaClasses selected = predicate != null ? classes.that(predicate) : classes;
        List<ConditionEvent> violations = new ArrayList<>();
        if (selected.isEmpty() && !allowEmpty) {
            violations.add(ConditionEvent.violated("",
                    "Rule failed to check any classes. This means either that no classes have been passed to the rule at all, "
                            + "or that no classes passed to the rule matched the `that()` clause. "
                            + "To allow rules being evaluated without checking any classes use allowEmptyShould(true)."));
        }
        ArchCondition<JavaClass> effective = negated ? condition.negate() : condition;
        for (JavaClass javaClass : selected) {
            ConditionEvents events = new ConditionEvents();
            effective.check(javaClass, events);
            violations.addAll(events.violations());
        }
        return new EvaluationResult(description(), violations);
    }

    @Override
    public ArchRule because(String newReason) {
        return new ClassesRule(negated, predicate, condition, newReason, allowEmpty, overriddenDescription);
    }

    @Override
    public ArchRule allowEmptyShould(boolean allow) {
        return new ClassesRule(negated, predicate, condition, reason, allow, overriddenDescription);
    }

    @Override
    public ArchRule as(String newDescription) {
        return new ClassesRule(negated, predicate, condition, reason, allowEmpty, newDescription);
    }

    @Override
    public String toString() {
        return description();
    }
}

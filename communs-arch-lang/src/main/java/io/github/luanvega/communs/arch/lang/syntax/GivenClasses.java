package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;

/**
 * Todas as classes importadas, antes de qualquer filtro.
 */
public final class GivenClasses {

    private final boolean negated;

    GivenClasses(boolean negated) {
        this.negated = negated;
    }

    public ClassesThat<GivenClassesConjunction> that() {
        return new ClassesThat<>(predicate -> new GivenClassesConjunction(negated, predicate));
    }

    public GivenClassesConjunction that(DescribedPredicate<? super JavaClass> predicate) {
        return new GivenClassesConjunction(negated, DescribedPredicate.describe(predicate.description(), predicate));
    }

    public ClassesShould should() {
        return new ClassesShould(condition -> new ClassesShouldConjunction(ClassesRule.of(negated, null, condition)));
    }

    public ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition) {
        return should().satisfy(condition);
    }
}

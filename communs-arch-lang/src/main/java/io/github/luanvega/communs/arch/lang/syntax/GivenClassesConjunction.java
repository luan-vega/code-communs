package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;

/**
 * Classes já filtradas; dá para combinar mais filtros com {@code and()}/{@code or()} ou seguir para {@code should()}.
 * Os filtros são combinados da esquerda para a direita, sem precedência entre {@code and} e {@code or}.
 */
public final class GivenClassesConjunction {

    private final boolean negated;
    private final DescribedPredicate<JavaClass> predicate;

    GivenClassesConjunction(boolean negated, DescribedPredicate<JavaClass> predicate) {
        this.negated = negated;
        this.predicate = predicate;
    }

    public ClassesThat<GivenClassesConjunction> and() {
        return new ClassesThat<>(this::and);
    }

    public ClassesThat<GivenClassesConjunction> or() {
        return new ClassesThat<>(this::or);
    }

    public GivenClassesConjunction and(DescribedPredicate<? super JavaClass> other) {
        return new GivenClassesConjunction(negated, predicate.and(other));
    }

    public GivenClassesConjunction or(DescribedPredicate<? super JavaClass> other) {
        return new GivenClassesConjunction(negated, predicate.or(other));
    }

    public ClassesShould should() {
        return new ClassesShould(condition -> new ClassesShouldConjunction(ClassesRule.of(negated, predicate, condition)));
    }

    public ClassesShouldConjunction should(ArchCondition<? super JavaClass> condition) {
        return should().satisfy(condition);
    }
}

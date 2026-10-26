package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.lang.ClassPredicates;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;
import java.util.function.Function;

/**
 * Filtros fluentes sobre classes. É usado tanto para escolher as classes da regra
 * ({@code classes().that()...}) quanto para descrever alvos de dependências
 * ({@code should().dependOnClassesThat()...}).
 *
 * @param <R> o próximo passo da DSL
 */
public final class ClassesThat<R> {

    private final Function<DescribedPredicate<JavaClass>, R> next;

    ClassesThat(Function<DescribedPredicate<JavaClass>, R> next) {
        this.next = next;
    }

    public R resideInAPackage(String packagePattern) {
        return next.apply(ClassPredicates.resideInAPackage(packagePattern));
    }

    public R resideInAnyPackage(String... packagePatterns) {
        return next.apply(ClassPredicates.resideInAnyPackage(packagePatterns));
    }

    public R resideOutsideOfPackage(String packagePattern) {
        return next.apply(ClassPredicates.resideOutsideOfPackage(packagePattern));
    }

    public R haveFullyQualifiedName(String name) {
        return next.apply(ClassPredicates.haveFullyQualifiedName(name));
    }

    public R haveSimpleName(String simpleName) {
        return next.apply(ClassPredicates.haveSimpleName(simpleName));
    }

    public R haveSimpleNameStartingWith(String prefix) {
        return next.apply(ClassPredicates.haveSimpleNameStartingWith(prefix));
    }

    public R haveSimpleNameEndingWith(String suffix) {
        return next.apply(ClassPredicates.haveSimpleNameEndingWith(suffix));
    }

    public R haveSimpleNameContaining(String infix) {
        return next.apply(ClassPredicates.haveSimpleNameContaining(infix));
    }

    public R haveNameMatching(String regex) {
        return next.apply(ClassPredicates.haveNameMatching(regex));
    }

    public R areAnnotatedWith(Class<?> annotationType) {
        return next.apply(ClassPredicates.areAnnotatedWith(annotationType));
    }

    public R areAnnotatedWith(String annotationTypeName) {
        return next.apply(ClassPredicates.areAnnotatedWith(annotationTypeName));
    }

    public R areNotAnnotatedWith(Class<?> annotationType) {
        return next.apply(DescribedPredicate.not(ClassPredicates.areAnnotatedWith(annotationType)));
    }

    public R areAssignableTo(Class<?> type) {
        return next.apply(ClassPredicates.areAssignableTo(type));
    }

    public R areAssignableTo(String typeName) {
        return next.apply(ClassPredicates.areAssignableTo(typeName));
    }

    public R areInterfaces() {
        return next.apply(ClassPredicates.areInterfaces());
    }

    public R areNotInterfaces() {
        return next.apply(DescribedPredicate.not(ClassPredicates.areInterfaces()));
    }

    public R areEnums() {
        return next.apply(ClassPredicates.areEnums());
    }

    public R areRecords() {
        return next.apply(ClassPredicates.areRecords());
    }

    public R arePublic() {
        return next.apply(ClassPredicates.arePublic());
    }

    public R areNested() {
        return next.apply(ClassPredicates.areNested());
    }

    public R areNotNested() {
        return next.apply(DescribedPredicate.not(ClassPredicates.areNested()));
    }

    /**
     * Filtro próprio, para casos que a DSL não cobre.
     */
    public R match(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(DescribedPredicate.describe(predicate.description(), predicate));
    }
}

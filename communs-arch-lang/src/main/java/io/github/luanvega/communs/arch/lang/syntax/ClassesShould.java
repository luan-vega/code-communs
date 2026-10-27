package io.github.luanvega.communs.arch.lang.syntax;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.ClassConditions;
import io.github.luanvega.communs.arch.lang.ClassPredicates;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;
import java.util.function.Function;

/**
 * Condições fluentes que as classes selecionadas devem atender
 * (ou, em {@code noClasses()}, não podem atender).
 */
public final class ClassesShould {

    private final Function<ArchCondition<JavaClass>, ClassesShouldConjunction> next;

    ClassesShould(Function<ArchCondition<JavaClass>, ClassesShouldConjunction> next) {
        this.next = next;
    }

    public ClassesShouldConjunction satisfy(ArchCondition<? super JavaClass> condition) {
        return next.apply(ArchCondition.from(condition.description(), condition::check));
    }

    public ClassesShouldConjunction be(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.be(predicate));
    }

    public ClassesShouldConjunction have(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.have(predicate));
    }

    public ClassesShouldConjunction resideInAPackage(String packagePattern) {
        return have(ClassPredicates.resideInAPackage(packagePattern));
    }

    public ClassesShouldConjunction resideInAnyPackage(String... packagePatterns) {
        return have(ClassPredicates.resideInAnyPackage(packagePatterns));
    }

    public ClassesShouldConjunction resideOutsideOfPackage(String packagePattern) {
        return have(ClassPredicates.resideOutsideOfPackage(packagePattern));
    }

    public ClassesShouldConjunction haveSimpleName(String simpleName) {
        return have(ClassPredicates.haveSimpleName(simpleName));
    }

    public ClassesShouldConjunction haveSimpleNameStartingWith(String prefix) {
        return have(ClassPredicates.haveSimpleNameStartingWith(prefix));
    }

    public ClassesShouldConjunction haveSimpleNameEndingWith(String suffix) {
        return have(ClassPredicates.haveSimpleNameEndingWith(suffix));
    }

    public ClassesShouldConjunction haveNameMatching(String regex) {
        return have(ClassPredicates.haveNameMatching(regex));
    }

    public ClassesShouldConjunction beAnnotatedWith(Class<?> annotationType) {
        return be(ClassPredicates.areAnnotatedWith(annotationType).as("annotated with @" + annotationType.getSimpleName()));
    }

    public ClassesShouldConjunction beAnnotatedWith(String annotationTypeName) {
        return be(ClassPredicates.areAnnotatedWith(annotationTypeName).as("annotated with @" + annotationTypeName));
    }

    public ClassesShouldConjunction beAssignableTo(Class<?> type) {
        return be(ClassPredicates.areAssignableTo(type).as("assignable to " + type.getName()));
    }

    public ClassesShouldConjunction beInterfaces() {
        return be(ClassPredicates.areInterfaces().as("interfaces"));
    }

    public ClassesShouldConjunction beRecords() {
        return be(ClassPredicates.areRecords().as("records"));
    }

    public ClassesShouldConjunction bePublic() {
        return be(ClassPredicates.arePublic().as("public"));
    }

    public ClassesShouldConjunction beFinal() {
        return be(ClassPredicates.areFinal().as("final"));
    }

    public ClassesShouldConjunction dependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.dependOnClassesThat(predicate));
    }

    public ClassesThat<ClassesShouldConjunction> dependOnClassesThat() {
        return new ClassesThat<>(this::dependOnClassesThat);
    }

    public ClassesShouldConjunction onlyDependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.onlyDependOnClassesThat(predicate));
    }

    public ClassesThat<ClassesShouldConjunction> onlyDependOnClassesThat() {
        return new ClassesThat<>(this::onlyDependOnClassesThat);
    }

    public ClassesShouldConjunction accessClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.accessClassesThat(predicate));
    }

    public ClassesThat<ClassesShouldConjunction> accessClassesThat() {
        return new ClassesThat<>(this::accessClassesThat);
    }

    public ClassesShouldConjunction onlyBeAccessedByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return next.apply(ClassConditions.onlyBeAccessedByClassesThat(predicate));
    }

    public ClassesThat<ClassesShouldConjunction> onlyBeAccessedByClassesThat() {
        return new ClassesThat<>(this::onlyBeAccessedByClassesThat);
    }

    public ClassesShouldConjunction haveOnlyFinalFields() {
        return next.apply(ClassConditions.haveOnlyFinalFields());
    }

    public ClassesShouldConjunction haveOnlyPrivateConstructors() {
        return next.apply(ClassConditions.haveOnlyPrivateConstructors());
    }

    public ClassesShouldConjunction haveFieldsAnnotatedWith(Class<?> annotationType) {
        return next.apply(ClassConditions.haveFieldsAnnotatedWith(annotationType.getName()));
    }

    public ClassesShouldConjunction haveFieldsAnnotatedWith(String annotationTypeName) {
        return next.apply(ClassConditions.haveFieldsAnnotatedWith(annotationTypeName));
    }
}

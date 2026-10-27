package io.github.luanvega.communs.arch.lang;

import io.github.luanvega.communs.arch.core.domain.Dependency;
import io.github.luanvega.communs.arch.core.domain.DependencyType;
import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaField;
import io.github.luanvega.communs.arch.core.domain.JavaMethod;
import io.github.luanvega.communs.arch.core.domain.JavaModifier;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Condições prontas sobre {@link JavaClass}. Cada condição registra os eventos atendidos e violados,
 * para que {@code noClasses()} possa invertê-las.
 */
public final class ClassConditions {

    private static final Set<DependencyType> ACCESS_TYPES =
            EnumSet.of(DependencyType.METHOD_CALL, DependencyType.CONSTRUCTOR_CALL, DependencyType.FIELD_ACCESS);

    private ClassConditions() {
    }

    /**
     * Condição atendida quando a classe atende o predicado, descrito como adjetivo
     * (ex.: {@code be(areInterfaces().as("interfaces"))} gera "be interfaces").
     */
    public static ArchCondition<JavaClass> be(DescribedPredicate<? super JavaClass> predicate) {
        return predicateCondition("be " + predicate.description(), predicate, "is ", "is not ");
    }

    /**
     * Condição atendida quando a classe atende o predicado, descrito como verbo
     * (ex.: {@code have(haveSimpleNameEndingWith("Service"))}).
     */
    public static ArchCondition<JavaClass> have(DescribedPredicate<? super JavaClass> predicate) {
        return predicateCondition(predicate.description(), predicate, "does ", "does not ");
    }

    private static ArchCondition<JavaClass> predicateCondition(
            String description, DescribedPredicate<? super JavaClass> predicate, String yes, String no) {
        String text = predicate.description();
        return ArchCondition.from(description, (javaClass, events) -> {
            boolean matches = predicate.test(javaClass);
            String message = "Class <" + javaClass.name() + "> " + (matches ? yes : no) + text + location(javaClass);
            events.add(new ConditionEvent(!matches, message, javaClass.name(), 0));
        });
    }

    /**
     * Atendida quando a classe depende (de qualquer forma) de alguma classe que atende o predicado.
     */
    public static ArchCondition<JavaClass> dependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return dependencyCondition("depend on classes that " + predicate.description(), predicate, dependency -> true, "depends on");
    }

    /**
     * Atendida quando a classe chama métodos ou construtores, ou acessa campos, de alguma classe que atende o predicado.
     * Diferente de {@link #dependOnClassesThat}, ignora dependências só de assinatura (campos, parâmetros, anotações).
     */
    public static ArchCondition<JavaClass> accessClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return dependencyCondition("access classes that " + predicate.description(), predicate,
                dependency -> ACCESS_TYPES.contains(dependency.type()), "accesses");
    }

    /**
     * Atendida quando todas as dependências da classe apontam para classes que atendem o predicado.
     */
    public static ArchCondition<JavaClass> onlyDependOnClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return ArchCondition.from("only depend on classes that " + predicate.description(), (javaClass, events) -> {
            List<Dependency> outside = javaClass.dependencies().stream()
                    .filter(dependency -> !predicate.test(target(javaClass, dependency)))
                    .sorted(ClassConditions::byLineAndTarget)
                    .toList();
            if (outside.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(),
                        "Class <" + javaClass.name() + "> only depends on classes that " + predicate.description()));
            }
            outside.forEach(dependency -> events.add(dependencyEvent(javaClass, dependency, true, "depends on")));
        });
    }

    /**
     * Atendida quando todas as classes importadas que acessam esta (chamadas, construtores, campos) atendem o predicado.
     */
    public static ArchCondition<JavaClass> onlyBeAccessedByClassesThat(DescribedPredicate<? super JavaClass> predicate) {
        return ArchCondition.from("only be accessed by classes that " + predicate.description(), (javaClass, events) -> {
            List<Dependency> outside = javaClass.dependenciesToSelf().stream()
                    .filter(dependency -> ACCESS_TYPES.contains(dependency.type()))
                    .filter(dependency -> !predicate.test(javaClass.resolve(dependency.originClassName())))
                    .sorted(Comparator.comparing(Dependency::originClassName).thenComparingInt(Dependency::lineNumber))
                    .toList();
            if (outside.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(),
                        "Class <" + javaClass.name() + "> is only accessed by classes that " + predicate.description()));
            }
            outside.forEach(dependency -> {
                JavaClass origin = javaClass.resolve(dependency.originClassName());
                events.add(dependencyEvent(origin, dependency, true, "accesses"));
            });
        });
    }

    public static ArchCondition<JavaClass> haveOnlyFinalFields() {
        return fieldCondition("have only final fields",
                field -> field.modifiers().contains(JavaModifier.FINAL) || field.modifiers().contains(JavaModifier.SYNTHETIC),
                "is not final");
    }

    public static ArchCondition<JavaClass> haveOnlyPrivateConstructors() {
        return ArchCondition.from("have only private constructors", (javaClass, events) -> {
            List<JavaMethod> nonPrivate = javaClass.constructors().stream()
                    .filter(constructor -> !constructor.modifiers().contains(JavaModifier.PRIVATE))
                    .toList();
            if (nonPrivate.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(), "Class <" + javaClass.name() + "> has only private constructors"));
            }
            nonPrivate.forEach(constructor -> events.add(ConditionEvent.violated(javaClass.name(),
                    "Constructor <" + javaClass.name() + "(" + String.join(", ", constructor.parameterTypeNames()) + ")> is not private"
                            + location(javaClass))));
        });
    }

    /**
     * Atendida quando algum campo da classe tem a anotação. Útil negada: {@code noClasses().should().haveFieldsAnnotatedWith(Autowired.class)}.
     */
    public static ArchCondition<JavaClass> haveFieldsAnnotatedWith(String annotationTypeName) {
        String annotation = "@" + ClassPredicates.simpleName(annotationTypeName);
        return ArchCondition.from("have fields annotated with " + annotation, (javaClass, events) -> {
            List<JavaField> annotated = javaClass.fields().stream().filter(field -> field.isAnnotatedWith(annotationTypeName)).toList();
            if (annotated.isEmpty()) {
                events.add(ConditionEvent.violated(javaClass.name(),
                        "Class <" + javaClass.name() + "> has no fields annotated with " + annotation + location(javaClass)));
            }
            annotated.forEach(field -> events.add(ConditionEvent.satisfied(javaClass.name(),
                    "Field <" + javaClass.name() + "." + field.name() + "> is annotated with " + annotation + location(javaClass))));
        });
    }

    private static ArchCondition<JavaClass> fieldCondition(String description, Predicate<JavaField> ok, String failure) {
        return ArchCondition.from(description, (javaClass, events) -> {
            List<JavaField> failing = javaClass.fields().stream().filter(ok.negate()).toList();
            if (failing.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(), "Class <" + javaClass.name() + "> " + description.replaceFirst("^have", "has")));
            }
            failing.forEach(field -> events.add(ConditionEvent.violated(javaClass.name(),
                    "Field <" + javaClass.name() + "." + field.name() + "> " + failure + location(javaClass))));
        });
    }

    private static ArchCondition<JavaClass> dependencyCondition(
            String description,
            DescribedPredicate<? super JavaClass> predicate,
            Predicate<Dependency> relevant,
            String verb) {
        return ArchCondition.from(description, (javaClass, events) -> {
            List<Dependency> matching = javaClass.dependencies().stream()
                    .filter(relevant)
                    .filter(dependency -> predicate.test(target(javaClass, dependency)))
                    .sorted(ClassConditions::byLineAndTarget)
                    .toList();
            if (matching.isEmpty()) {
                events.add(ConditionEvent.violated(javaClass.name(),
                        "Class <" + javaClass.name() + "> does not " + description + location(javaClass)));
            }
            matching.forEach(dependency -> events.add(dependencyEvent(javaClass, dependency, false, verb)));
        });
    }

    private static ConditionEvent dependencyEvent(JavaClass origin, Dependency dependency, boolean violation, String verb) {
        String message = "Class <" + origin.name() + "> " + verb + " <" + dependency.targetClassName() + "> ["
                + dependency.type() + "]" + location(origin, dependency.lineNumber());
        return new ConditionEvent(violation, message, origin.name(), dependency.lineNumber());
    }

    private static JavaClass target(JavaClass origin, Dependency dependency) {
        return origin.resolve(dependency.targetClassName());
    }

    private static int byLineAndTarget(Dependency a, Dependency b) {
        int byLine = Integer.compare(a.lineNumber(), b.lineNumber());
        return byLine != 0 ? byLine : a.targetClassName().compareTo(b.targetClassName());
    }

    private static String location(JavaClass javaClass) {
        return location(javaClass, 0);
    }

    private static String location(JavaClass javaClass, int line) {
        String file = javaClass.sourceFileName().orElse(javaClass.simpleName() + ".java");
        return " in (" + file + ":" + Math.max(line, 0) + ")";
    }
}

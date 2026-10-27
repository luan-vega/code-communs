package io.github.luanvega.communs.arch.core.domain;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Conjunto imutável de classes importadas, indexado pelo nome.
 */
public final class JavaClasses implements Iterable<JavaClass> {

    private final Map<String, JavaClass> byName;
    private final ClassResolver resolver;

    private JavaClasses(Map<String, JavaClass> byName, ClassResolver resolver) {
        this.byName = byName;
        this.resolver = resolver;
    }

    /**
     * Cria o conjunto e liga cada classe a ele, para resolver hierarquias e dependências.
     */
    public static JavaClasses of(Collection<JavaClass> classes) {
        Map<String, JavaClass> byName = sortedByName(classes);
        ClassResolver resolver = new ClassResolver(byName);
        byName.values().forEach(javaClass -> javaClass.attach(resolver));
        return new JavaClasses(byName, resolver);
    }

    private static Map<String, JavaClass> sortedByName(Collection<JavaClass> classes) {
        Map<String, JavaClass> byName = new LinkedHashMap<>();
        classes.stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .forEach(javaClass -> byName.putIfAbsent(javaClass.name(), javaClass));
        return byName;
    }

    /**
     * Devolve a classe importada com esse nome ou, se ela não foi importada, uma classe só com o nome
     * ({@link JavaClass#isImported()} {@code false}). Útil para avaliar o alvo de dependências.
     */
    public JavaClass resolve(String name) {
        return resolver.resolve(name);
    }

    public Optional<JavaClass> get(String name) {
        return Optional.ofNullable(byName.get(name));
    }

    public Optional<JavaClass> get(Class<?> type) {
        return get(type.getName());
    }

    public boolean contain(String name) {
        return byName.containsKey(name);
    }

    public boolean contain(Class<?> type) {
        return contain(type.getName());
    }

    /**
     * Filtra as classes. O resultado continua resolvendo hierarquias e dependências pelo conjunto original.
     */
    public JavaClasses that(Predicate<? super JavaClass> predicate) {
        return new JavaClasses(sortedByName(stream().filter(predicate).toList()), resolver);
    }

    public int size() {
        return byName.size();
    }

    public boolean isEmpty() {
        return byName.isEmpty();
    }

    public Stream<JavaClass> stream() {
        return byName.values().stream();
    }

    @Override
    public Iterator<JavaClass> iterator() {
        return byName.values().iterator();
    }

    @Override
    public String toString() {
        return "JavaClasses" + byName.keySet();
    }
}

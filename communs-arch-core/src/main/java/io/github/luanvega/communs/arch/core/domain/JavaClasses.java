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

    private JavaClasses(Map<String, JavaClass> byName) {
        this.byName = byName;
    }

    public static JavaClasses of(Collection<JavaClass> classes) {
        Map<String, JavaClass> byName = new LinkedHashMap<>();
        classes.stream()
                .sorted((a, b) -> a.name().compareTo(b.name()))
                .forEach(javaClass -> byName.putIfAbsent(javaClass.name(), javaClass));
        return new JavaClasses(byName);
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

    public JavaClasses that(Predicate<? super JavaClass> predicate) {
        return of(stream().filter(predicate).toList());
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

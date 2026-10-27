package io.github.luanvega.communs.arch.core.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Resolve nomes de classes para {@link JavaClass}: usa as classes importadas e, para as demais,
 * cria classes "stub" (só com o nome) e consulta a hierarquia por reflexão quando a classe está no classpath.
 */
final class ClassResolver {

    private final Map<String, JavaClass> imported;
    private final Map<String, JavaClass> stubs = new ConcurrentHashMap<>();
    private final Map<String, List<String>> reflectedSuperTypes = new ConcurrentHashMap<>();
    private volatile Map<String, Set<Dependency>> dependenciesByTarget;

    ClassResolver(Map<String, JavaClass> imported) {
        this.imported = Map.copyOf(imported);
    }

    JavaClass resolve(String name) {
        JavaClass javaClass = imported.get(name);
        if (javaClass != null) {
            return javaClass;
        }
        return stubs.computeIfAbsent(name, this::stub);
    }

    Optional<JavaClass> imported(String name) {
        return Optional.ofNullable(imported.get(name));
    }

    Set<Dependency> dependenciesTo(String name) {
        Map<String, Set<Dependency>> index = dependenciesByTarget;
        if (index == null) {
            index = imported.values().stream()
                    .flatMap(javaClass -> javaClass.dependencies().stream())
                    .collect(Collectors.groupingBy(Dependency::targetClassName, Collectors.toUnmodifiableSet()));
            dependenciesByTarget = index;
        }
        return index.getOrDefault(name, Set.of());
    }

    /**
     * Todos os supertipos diretos e indiretos (classes e interfaces), sem incluir o próprio tipo.
     */
    Set<String> allSuperTypeNames(String name) {
        Set<String> visited = new HashSet<>();
        Deque<String> pending = new ArrayDeque<>(directSuperTypeNames(name));
        while (!pending.isEmpty()) {
            String current = pending.pop();
            if (visited.add(current)) {
                pending.addAll(directSuperTypeNames(current));
            }
        }
        return visited;
    }

    private List<String> directSuperTypeNames(String name) {
        JavaClass javaClass = imported.get(name);
        if (javaClass != null) {
            List<String> superTypes = new ArrayList<>();
            javaClass.superclassName().ifPresent(superTypes::add);
            superTypes.addAll(javaClass.interfaceNames());
            return superTypes;
        }
        return reflectedSuperTypes.computeIfAbsent(name, ClassResolver::reflectSuperTypes);
    }

    private static List<String> reflectSuperTypes(String name) {
        return loadClass(name).map(type -> {
            List<String> superTypes = new ArrayList<>();
            if (type.getSuperclass() != null) {
                superTypes.add(type.getSuperclass().getName());
            }
            for (Class<?> anInterface : type.getInterfaces()) {
                superTypes.add(anInterface.getName());
            }
            return superTypes;
        }).orElse(List.of());
    }

    private JavaClass stub(String name) {
        JavaClassKind kind = loadClass(name).map(ClassResolver::kindOf).orElse(JavaClassKind.CLASS);
        JavaClass stub = new JavaClass(name, kind, Set.of(), null, List.of(), List.of(), List.of(), List.of(), Set.of(), null, false);
        stub.attach(this);
        return stub;
    }

    private static JavaClassKind kindOf(Class<?> type) {
        if (type.isAnnotation()) {
            return JavaClassKind.ANNOTATION;
        }
        if (type.isInterface()) {
            return JavaClassKind.INTERFACE;
        }
        if (type.isEnum()) {
            return JavaClassKind.ENUM;
        }
        if (type.isRecord()) {
            return JavaClassKind.RECORD;
        }
        return JavaClassKind.CLASS;
    }

    private static Optional<Class<?>> loadClass(String name) {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        try {
            return Optional.of(Class.forName(name, false, classLoader != null ? classLoader : ClassResolver.class.getClassLoader()));
        } catch (ClassNotFoundException | LinkageError e) {
            return Optional.empty();
        }
    }
}

package io.github.luanvega.communs.arch.core.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Classe importada do bytecode, com seus membros, anotações e dependências.
 * Classes aninhadas usam o nome binário, com {@code $} (ex.: {@code com.exemplo.Pedido$Item}).
 */
public final class JavaClass {

    private final String name;
    private final JavaClassKind kind;
    private final Set<JavaModifier> modifiers;
    private final String superclassName;
    private final List<String> interfaceNames;
    private final List<JavaAnnotation> annotations;
    private final List<JavaField> fields;
    private final List<JavaMethod> methods;
    private final Set<Dependency> dependencies;
    private final String sourceFileName;

    public JavaClass(
            String name,
            JavaClassKind kind,
            Set<JavaModifier> modifiers,
            String superclassName,
            List<String> interfaceNames,
            List<JavaAnnotation> annotations,
            List<JavaField> fields,
            List<JavaMethod> methods,
            Set<Dependency> dependencies,
            String sourceFileName) {
        this.name = Objects.requireNonNull(name, "name");
        this.kind = Objects.requireNonNull(kind, "kind");
        this.modifiers = Set.copyOf(modifiers);
        this.superclassName = superclassName;
        this.interfaceNames = List.copyOf(interfaceNames);
        this.annotations = List.copyOf(annotations);
        this.fields = List.copyOf(fields);
        this.methods = List.copyOf(methods);
        this.dependencies = Set.copyOf(dependencies);
        this.sourceFileName = sourceFileName;
    }

    public String name() {
        return name;
    }

    public String simpleName() {
        String withoutPackage = name.substring(name.lastIndexOf('.') + 1);
        return withoutPackage.substring(withoutPackage.lastIndexOf('$') + 1);
    }

    public String packageName() {
        int lastDot = name.lastIndexOf('.');
        return lastDot < 0 ? "" : name.substring(0, lastDot);
    }

    public JavaClassKind kind() {
        return kind;
    }

    public Set<JavaModifier> modifiers() {
        return modifiers;
    }

    public Optional<String> superclassName() {
        return Optional.ofNullable(superclassName);
    }

    public List<String> interfaceNames() {
        return interfaceNames;
    }

    public List<JavaAnnotation> annotations() {
        return annotations;
    }

    public List<JavaField> fields() {
        return fields;
    }

    public List<JavaMethod> methods() {
        return methods;
    }

    public List<JavaMethod> constructors() {
        return methods.stream().filter(JavaMethod::isConstructor).toList();
    }

    public Set<Dependency> dependencies() {
        return dependencies;
    }

    public Optional<String> sourceFileName() {
        return Optional.ofNullable(sourceFileName);
    }

    public boolean isNested() {
        return name.indexOf('$', name.lastIndexOf('.') + 1) >= 0;
    }

    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.stream().anyMatch(a -> a.typeName().equals(annotationTypeName));
    }

    public boolean isAnnotatedWith(Class<?> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }

    public boolean dependsOn(String targetClassName) {
        return dependencies.stream().anyMatch(d -> d.targetClassName().equals(targetClassName));
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof JavaClass that && name.equals(that.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return "JavaClass[" + name + "]";
    }
}

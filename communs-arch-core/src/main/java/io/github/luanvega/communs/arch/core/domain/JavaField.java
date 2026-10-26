package io.github.luanvega.communs.arch.core.domain;

import java.util.List;
import java.util.Set;

/**
 * Campo declarado numa classe.
 *
 * @param name nome do campo
 * @param typeName nome totalmente qualificado do tipo (sem genéricos)
 */
public record JavaField(String name, String typeName, Set<JavaModifier> modifiers, List<JavaAnnotation> annotations) {

    public JavaField {
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
    }

    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.stream().anyMatch(a -> a.typeName().equals(annotationTypeName));
    }

    public boolean isAnnotatedWith(Class<?> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }
}

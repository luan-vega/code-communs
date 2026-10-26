package io.github.luanvega.communs.arch.core.domain;

import java.util.List;
import java.util.Set;

/**
 * Método ou construtor declarado numa classe.
 *
 * @param name nome do método; construtores usam {@value #CONSTRUCTOR_NAME}
 * @param returnTypeName nome totalmente qualificado do tipo de retorno ({@code void} quando não há)
 * @param parameterTypeNames tipos dos parâmetros, em ordem
 * @param throwsTypeNames exceções declaradas em {@code throws}
 */
public record JavaMethod(
        String name,
        String returnTypeName,
        List<String> parameterTypeNames,
        List<String> throwsTypeNames,
        Set<JavaModifier> modifiers,
        List<JavaAnnotation> annotations) {

    public static final String CONSTRUCTOR_NAME = "<init>";
    public static final String STATIC_INITIALIZER_NAME = "<clinit>";

    public JavaMethod {
        parameterTypeNames = List.copyOf(parameterTypeNames);
        throwsTypeNames = List.copyOf(throwsTypeNames);
        modifiers = Set.copyOf(modifiers);
        annotations = List.copyOf(annotations);
    }

    public boolean isConstructor() {
        return CONSTRUCTOR_NAME.equals(name);
    }

    public boolean isStaticInitializer() {
        return STATIC_INITIALIZER_NAME.equals(name);
    }

    public boolean isAnnotatedWith(String annotationTypeName) {
        return annotations.stream().anyMatch(a -> a.typeName().equals(annotationTypeName));
    }

    public boolean isAnnotatedWith(Class<?> annotationType) {
        return isAnnotatedWith(annotationType.getName());
    }
}

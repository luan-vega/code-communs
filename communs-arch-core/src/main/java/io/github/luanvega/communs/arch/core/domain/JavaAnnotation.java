package io.github.luanvega.communs.arch.core.domain;

import java.util.Objects;

/**
 * Anotação presente numa classe, campo, método ou parâmetro.
 *
 * @param typeName nome totalmente qualificado do tipo da anotação
 */
public record JavaAnnotation(String typeName) {

    public JavaAnnotation {
        Objects.requireNonNull(typeName, "typeName");
    }

    public boolean isOfType(Class<?> type) {
        return typeName.equals(type.getName());
    }
}

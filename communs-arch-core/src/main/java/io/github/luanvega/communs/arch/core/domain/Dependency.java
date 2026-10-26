package io.github.luanvega.communs.arch.core.domain;

import java.util.Objects;

/**
 * Dependência de uma classe importada para outro tipo, importado ou não.
 *
 * @param originClassName classe que depende
 * @param targetClassName tipo do qual ela depende
 * @param type como a dependência acontece
 * @param lineNumber linha no código-fonte, ou {@code 0} quando o bytecode não informa
 */
public record Dependency(String originClassName, String targetClassName, DependencyType type, int lineNumber) {

    public Dependency {
        Objects.requireNonNull(originClassName, "originClassName");
        Objects.requireNonNull(targetClassName, "targetClassName");
        Objects.requireNonNull(type, "type");
    }

    public String describe() {
        String location = lineNumber > 0 ? " (linha " + lineNumber + ")" : "";
        return originClassName + " -> " + targetClassName + " [" + type + "]" + location;
    }
}

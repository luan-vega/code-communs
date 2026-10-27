package io.github.luanvega.communs.arch.core.domain;

import java.util.Objects;

/**
 * Dependência de uma classe importada para outro tipo, importado ou não.
 *
 * @param originClassName classe que depende
 * @param targetClassName tipo do qual ela depende
 * @param type como a dependência acontece
 * @param lineNumber linha no código-fonte, ou {@code 0} quando o bytecode não informa
 * @param targetMemberName método ou campo acessado no alvo (ex.: {@code println}, {@code out}), ou vazio
 *                         quando a dependência não é um acesso a membro
 */
public record Dependency(String originClassName, String targetClassName, DependencyType type, int lineNumber, String targetMemberName) {

    public Dependency {
        Objects.requireNonNull(originClassName, "originClassName");
        Objects.requireNonNull(targetClassName, "targetClassName");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(targetMemberName, "targetMemberName");
    }

    public Dependency(String originClassName, String targetClassName, DependencyType type, int lineNumber) {
        this(originClassName, targetClassName, type, lineNumber, "");
    }

    /**
     * O alvo com o membro, quando houver (ex.: {@code java.lang.System.out}).
     */
    public String target() {
        return targetMemberName.isEmpty() ? targetClassName : targetClassName + "." + targetMemberName;
    }

    public String describe() {
        String location = lineNumber > 0 ? " (linha " + lineNumber + ")" : "";
        return originClassName + " -> " + target() + " [" + type + "]" + location;
    }
}

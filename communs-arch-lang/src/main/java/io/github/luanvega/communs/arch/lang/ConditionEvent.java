package io.github.luanvega.communs.arch.lang;

import java.util.Objects;

/**
 * Resultado de uma condição para um item: atendida ou violada, com a mensagem que explica o porquê.
 *
 * @param violation {@code true} quando a condição foi violada
 * @param message explicação, ex.: "Class a.B calls method c.D.save() in (B.java:12)"
 * @param className classe onde o evento aconteceu
 * @param lineNumber linha no código-fonte, ou {@code 0} quando não se aplica
 */
public record ConditionEvent(boolean violation, String message, String className, int lineNumber) {

    public ConditionEvent {
        Objects.requireNonNull(message, "message");
        Objects.requireNonNull(className, "className");
    }

    public static ConditionEvent satisfied(String className, String message) {
        return new ConditionEvent(false, message, className, 0);
    }

    public static ConditionEvent violated(String className, String message) {
        return new ConditionEvent(true, message, className, 0);
    }

    public ConditionEvent atLine(int line) {
        return new ConditionEvent(violation, message, className, line);
    }

    /**
     * O mesmo evento com o resultado invertido, usado por {@code noClasses()}.
     */
    public ConditionEvent inverted() {
        return new ConditionEvent(!violation, message, className, lineNumber);
    }
}

package io.github.luanvega.communs.arch.lang.syntax;

/**
 * Ponto de entrada da DSL: {@code classes()} seleciona classes que devem atender uma condição,
 * {@code noClasses()} seleciona classes que não podem atendê-la.
 */
public final class ArchRuleDefinition {

    private ArchRuleDefinition() {
    }

    public static GivenClasses classes() {
        return new GivenClasses(false);
    }

    public static GivenClasses noClasses() {
        return new GivenClasses(true);
    }
}

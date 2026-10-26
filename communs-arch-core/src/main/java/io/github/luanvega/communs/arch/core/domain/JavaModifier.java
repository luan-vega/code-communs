package io.github.luanvega.communs.arch.core.domain;

import java.util.EnumSet;
import java.util.Set;

/**
 * Modificadores de classes, campos e métodos, lidos das flags de acesso do bytecode.
 */
public enum JavaModifier {
    PUBLIC(0x0001),
    PRIVATE(0x0002),
    PROTECTED(0x0004),
    STATIC(0x0008),
    FINAL(0x0010),
    SYNCHRONIZED(0x0020),
    VOLATILE(0x0040),
    TRANSIENT(0x0080),
    NATIVE(0x0100),
    ABSTRACT(0x0400),
    SYNTHETIC(0x1000);

    private final int flag;

    JavaModifier(int flag) {
        this.flag = flag;
    }

    /**
     * Converte as flags de acesso de uma classe.
     */
    public static Set<JavaModifier> ofClass(int access) {
        // Em classes, 0x0020 é ACC_SUPER, não SYNCHRONIZED.
        return of(access, EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, ABSTRACT, SYNTHETIC));
    }

    /**
     * Converte as flags de acesso de um campo.
     */
    public static Set<JavaModifier> ofField(int access) {
        return of(access, EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, VOLATILE, TRANSIENT, SYNTHETIC));
    }

    /**
     * Converte as flags de acesso de um método.
     */
    public static Set<JavaModifier> ofMethod(int access) {
        // Em métodos, 0x0040 é ACC_BRIDGE e 0x0080 é ACC_VARARGS.
        return of(access, EnumSet.of(PUBLIC, PRIVATE, PROTECTED, STATIC, FINAL, SYNCHRONIZED, NATIVE, ABSTRACT, SYNTHETIC));
    }

    private static Set<JavaModifier> of(int access, Set<JavaModifier> applicable) {
        Set<JavaModifier> modifiers = EnumSet.noneOf(JavaModifier.class);
        for (JavaModifier modifier : applicable) {
            if ((access & modifier.flag) != 0) {
                modifiers.add(modifier);
            }
        }
        return Set.copyOf(modifiers);
    }
}

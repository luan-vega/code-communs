package io.github.luanvega.communs.arch.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Opcodes;

class AsmDisponivelTest {

    @Test
    void asmLeBytecodeDoJava21() throws Exception {
        ClassReader leitor = new ClassReader(AsmDisponivelTest.class.getName());

        assertThat(leitor.getClassName()).isEqualTo("io/github/luanvega/communs/arch/core/AsmDisponivelTest");
        assertThat(Opcodes.V21).isPositive();
    }
}

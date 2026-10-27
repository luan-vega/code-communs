package io.github.luanvega.communs.arch.junit.exemplos;

import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.classes;

import io.github.luanvega.communs.arch.junit.AnalisarClasses;
import io.github.luanvega.communs.arch.junit.RegraArch;
import io.github.luanvega.communs.arch.lang.ArchRule;

/** Exemplo executado só pelo EngineTestKit. */
@AnalisarClasses(pacotesDe = io.github.luanvega.communs.arch.junit.fixtures.certo.service.PedidoService.class, incluirTestes = true)
public class DeclaracoesInvalidas {

    @RegraArch
    final ArchRule naoEstatico = classes().should().bePublic();

    @RegraArch
    static String retornoErrado() {
        return "x";
    }
}

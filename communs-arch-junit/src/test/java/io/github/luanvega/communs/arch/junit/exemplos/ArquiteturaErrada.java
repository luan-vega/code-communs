package io.github.luanvega.communs.arch.junit.exemplos;

import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;

import io.github.luanvega.communs.arch.junit.AnalisarClasses;
import io.github.luanvega.communs.arch.junit.RegraArch;
import io.github.luanvega.communs.arch.lang.ArchRule;

/** Exemplo executado só pelo EngineTestKit. */
@AnalisarClasses(pacotes = "io.github.luanvega.communs.arch.junit.fixtures.errado", incluirTestes = true, regrasPadrao = true,
        configuracao = "regras-sem-ciclos.yaml")
public class ArquiteturaErrada {

    @RegraArch
    static final ArchRule controllersNaoAcessamRepositorios = noClasses().that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..")
            .because("controllers falam só com services");
}

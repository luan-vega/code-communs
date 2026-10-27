package io.github.luanvega.communs.arch.junit.exemplos;

import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.junit.AnalisarClasses;
import io.github.luanvega.communs.arch.junit.RegraArch;
import io.github.luanvega.communs.arch.lang.ArchRule;

/** Exemplo executado só pelo EngineTestKit (o Surefire deste módulo exclui a engine communs-arch). */
@AnalisarClasses(pacotes = "io.github.luanvega.communs.arch.junit.fixtures.certo", incluirTestes = true, regrasPadrao = true)
public class ArquiteturaCerta {

    @RegraArch
    static final ArchRule controllersNaoAcessamRepositorios = noClasses().that().resideInAPackage("..controller..")
            .should().accessClassesThat().resideInAPackage("..repository..");

    @RegraArch
    static void importouAsTresCamadas(JavaClasses classes) {
        assertThat(classes.size()).isEqualTo(3);
    }

    @RegraArch
    void metodoDeInstancia(JavaClasses classes) {
        assertThat(classes.contain("io.github.luanvega.communs.arch.junit.fixtures.certo.service.PedidoService")).isTrue();
    }
}

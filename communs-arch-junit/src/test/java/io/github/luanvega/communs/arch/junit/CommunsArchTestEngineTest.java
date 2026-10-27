package io.github.luanvega.communs.arch.junit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectPackage;
import static org.junit.platform.testkit.engine.EventConditions.event;
import static org.junit.platform.testkit.engine.EventConditions.finishedWithFailure;
import static org.junit.platform.testkit.engine.EventConditions.test;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.instanceOf;
import static org.junit.platform.testkit.engine.TestExecutionResultConditions.message;

import io.github.luanvega.communs.arch.junit.exemplos.ArquiteturaCerta;
import io.github.luanvega.communs.arch.junit.exemplos.ArquiteturaErrada;
import io.github.luanvega.communs.arch.junit.exemplos.DeclaracoesInvalidas;
import io.github.luanvega.communs.arch.junit.exemplos.FalharEmAvisos;
import io.github.luanvega.communs.arch.lang.ArchRuleViolationError;
import org.junit.jupiter.api.Test;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

class CommunsArchTestEngineTest {

    @Test
    void rodaCamposMetodosERegrasDoCatalogo() {
        Events testes = executar(ArquiteturaCerta.class);

        // 3 regras próprias + 10 do catálogo; CODIGO-002 fica abortada por falta de excecaoBase.
        testes.assertStatistics(estatisticas -> estatisticas.started(13).succeeded(12).aborted(1).failed(0));
        assertThat(nome(testes.aborted().list().getFirst())).startsWith("CODIGO-002 ");
    }

    @Test
    void falhaComORelatorioDaViolacaoEAvisoViraAbortado() {
        Events testes = executar(ArquiteturaErrada.class);

        testes.assertThatEvents().haveExactly(1, event(test("controllersNaoAcessamRepositorios"),
                finishedWithFailure(instanceOf(ArchRuleViolationError.class),
                        message(m -> m.contains("because controllers falam só com services") && m.contains("PedidoController.java:11")))));
        testes.assertThatEvents().haveExactly(1, event(test("CAMADAS-001"),
                finishedWithFailure(instanceOf(ArchRuleViolationError.class), message(m -> m.contains("[CAMADAS-001]")))));
        assertThat(testes.aborted().list()).extracting(CommunsArchTestEngineTest::nome)
                .anyMatch(nome -> nome.startsWith("CODIGO-001 "))
                .anyMatch(nome -> nome.startsWith("CICLOS-001 "));
        assertThat(motivo(testes, "CODIGO-001")).startsWith("AVISO: Architecture Violation").contains("java.lang.System.out");
        assertThat(motivo(testes, "CICLOS-001")).isEqualTo("CICLOS-001 ignorada: desligada na configuração");
    }

    @Test
    void falharEmAvisosTransformaAvisoEmFalha() {
        Events testes = executar(FalharEmAvisos.class);

        testes.assertThatEvents().haveExactly(1, event(test("CODIGO-001"), finishedWithFailure(instanceOf(ArchRuleViolationError.class))));
    }

    @Test
    void explicaDeclaracoesInvalidas() {
        Events testes = executar(DeclaracoesInvalidas.class);

        testes.assertThatEvents()
                .haveExactly(1, event(test("naoEstatico"), finishedWithFailure(message(m -> m.contains("deve ser static")))))
                .haveExactly(1, event(test("retornoErrado"), finishedWithFailure(message(m -> m.contains("deve ser void")))));
    }

    @Test
    void encontraAsClassesPorPacote() {
        Events containers = EngineTestKit.engine(new CommunsArchTestEngine())
                .selectors(selectPackage("io.github.luanvega.communs.arch.junit.exemplos"))
                .execute()
                .containerEvents();

        assertThat(containers.started().list()).extracting(CommunsArchTestEngineTest::nome)
                .contains("ArquiteturaCerta", "ArquiteturaErrada", "FalharEmAvisos", "DeclaracoesInvalidas");
    }

    private static Events executar(Class<?> classe) {
        return EngineTestKit.engine(new CommunsArchTestEngine()).selectors(selectClass(classe)).execute().testEvents();
    }

    private static String nome(Event evento) {
        return evento.getTestDescriptor().getDisplayName();
    }

    private static String motivo(Events testes, String id) {
        return testes.aborted().list().stream()
                .filter(evento -> nome(evento).startsWith(id + " "))
                .map(evento -> evento.getRequiredPayload(TestExecutionResult.class).getThrowable().orElseThrow().getMessage())
                .findFirst()
                .orElseThrow();
    }
}

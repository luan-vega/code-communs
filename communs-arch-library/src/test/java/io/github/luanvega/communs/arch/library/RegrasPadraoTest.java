package io.github.luanvega.communs.arch.library;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Cada Regra padrão roda contra um projeto que a respeita ({@code fixtures.certo})
 * e outro que a viola ({@code fixtures.errado}).
 */
class RegrasPadraoTest {

    private static final String CERTO = "io.github.luanvega.communs.arch.library.fixtures.certo";
    private static final String ERRADO = "io.github.luanvega.communs.arch.library.fixtures.errado";

    private static JavaClasses certo;
    private static JavaClasses errado;

    @BeforeAll
    static void importar() {
        certo = new ClassFileImporter().importPackages(CERTO);
        errado = new ClassFileImporter().importPackages(ERRADO);
    }

    static Stream<Arguments> violacoesEsperadas() {
        return Stream.of(
                Arguments.of("CAMADAS-001", List.of(
                        "Class <" + ERRADO + ".controller.PedidoApi> accesses <" + ERRADO + ".repository.PedidoDao.findById> [METHOD_CALL] in (PedidoApi.java:19)")),
                Arguments.of("CAMADAS-002", List.of(
                        "Class <" + ERRADO + ".domain.Pedido> depends on <org.springframework.stereotype.Component> [ANNOTATION] in (Pedido.java:0)")),
                Arguments.of("CICLOS-001", List.of(
                        "Cycle detected between slices [domain, service] of '" + ERRADO + "':")),
                Arguments.of("NOMES-001", List.of(
                        "Class <" + ERRADO + ".controller.PedidoApi> does not have simple name ending with 'Controller' in (PedidoApi.java:0)")),
                Arguments.of("NOMES-002", List.of(
                        "Class <" + ERRADO + ".service.Pedidos> does not have simple name ending with 'Service' in (Pedidos.java:0)")),
                Arguments.of("NOMES-003", List.of(
                        "Class <" + ERRADO + ".repository.PedidoDao> does not have simple name ending with 'Repository' in (PedidoDao.java:0)")),
                Arguments.of("SPRING-001", List.of(
                        "Field <" + ERRADO + ".controller.PedidoApi.dao> is annotated with @Autowired in (PedidoApi.java:0)")),
                Arguments.of("SPRING-002", List.of(
                        "Method <" + ERRADO + ".controller.PedidoApi.get> returns entity <" + ERRADO + ".domain.Pedido> in (PedidoApi.java:0)")),
                Arguments.of("CODIGO-001", List.of(
                        "Class <" + ERRADO + ".controller.PedidoApi> uses <java.lang.System.out> in (PedidoApi.java:18)",
                        "Class <" + ERRADO + ".service.Pedidos> uses <java.lang.NumberFormatException.printStackTrace> in (Pedidos.java:14)")),
                Arguments.of("CODIGO-002", List.of(
                        "Class <" + ERRADO + ".domain.PedidoInvalido> is not assignable to " + ERRADO + ".domain.ErroDeNegocio in (PedidoInvalido.java:0)")));
    }

    @ParameterizedTest(name = "{0} passa no projeto certo")
    @MethodSource("violacoesEsperadas")
    void projetoCertoNaoViolaARegra(String id, List<String> ignorado) {
        ResultadoRegra resultado = avaliar(id, certo, CERTO);

        assertThat(resultado.foiIgnorada()).isFalse();
        assertThat(resultado.violacoes()).extracting(ConditionEvent::message).isEmpty();
    }

    @ParameterizedTest(name = "{0} aponta as violações do projeto errado")
    @MethodSource("violacoesEsperadas")
    void projetoErradoViolaARegra(String id, List<String> esperadas) {
        ResultadoRegra resultado = avaliar(id, errado, ERRADO);

        List<String> mensagens = resultado.violacoes().stream().map(ConditionEvent::message).toList();
        assertThat(mensagens).hasSameSizeAs(esperadas);
        for (int i = 0; i < esperadas.size(); i++) {
            assertThat(mensagens.get(i)).startsWith(esperadas.get(i));
        }
        assertThat(resultado.avaliacao().ruleDescription()).startsWith("[" + id + "] ");
    }

    private static ResultadoRegra avaliar(String id, JavaClasses classes, String pacote) {
        Regra regra = new RegrasPadrao().regras().stream().filter(r -> r.id().equals(id)).findFirst().orElseThrow();
        ConfiguracaoRegras config = ConfiguracaoRegras.builder().excecaoBase(pacote + ".domain.ErroDeNegocio").build();
        return RegrasRegistry.de(List.of(regra)).avaliar(classes, config).getFirst();
    }
}

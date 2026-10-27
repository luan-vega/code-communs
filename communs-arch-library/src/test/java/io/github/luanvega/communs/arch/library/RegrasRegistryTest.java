package io.github.luanvega.communs.arch.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RegrasRegistryTest {

    private static final String ERRADO = "io.github.luanvega.communs.arch.library.fixtures.errado";

    @Test
    void catalogoPadraoTemAsDezRegrasDoPlano() {
        assertThat(new RegrasPadrao().regras()).extracting(Regra::id).containsExactly(
                "CAMADAS-001", "CAMADAS-002", "CICLOS-001",
                "NOMES-001", "NOMES-002", "NOMES-003",
                "SPRING-001", "SPRING-002",
                "CODIGO-001", "CODIGO-002");
    }

    @Test
    void todaRegraPadraoTemDocumentacao() {
        assertThat(new RegrasPadrao().regras()).allSatisfy(regra -> assertThat(regra.documentacao())
                .as(regra.id())
                .hasValueSatisfying(texto -> assertThat(texto)
                        .startsWith("# " + regra.id() + " ")
                        .contains("## O que verifica", "## Por quê", "## Errado", "## Certo")));
    }

    @Test
    void carregaRegrasAlternativasPorServiceLoader() {
        RegrasRegistry registry = RegrasRegistry.padrao();

        assertThat(registry.buscar("EXTRA-001")).isPresent();
        assertThat(registry.todas()).hasSize(11);
        assertThat(registry.todas()).extracting(Regra::id).isSorted();
    }

    @Test
    void ignoraRegrasDesligadasOuSemConfiguracao() {
        JavaClasses errado = new ClassFileImporter().importPackages(ERRADO);
        ConfiguracaoRegras config = ConfiguracaoRegras.padrao().toBuilder().desligar("NOMES-001").build();

        List<ResultadoRegra> resultados = RegrasRegistry.de(new RegrasPadrao().regras()).avaliar(errado, config);

        assertThat(resultado(resultados, "NOMES-001").motivo()).contains("desligada na configuração");
        assertThat(resultado(resultados, "CODIGO-002").motivo()).contains("falta configuração para esta regra");
        assertThat(resultados).filteredOn(r -> !r.foiIgnorada()).allMatch(ResultadoRegra::temViolacoes);
    }

    @Test
    void recusaIdsRepetidosOuForaDoPadrao() {
        Regra regra = new RegrasPadrao().regras().getFirst();

        assertThatThrownBy(() -> RegrasRegistry.de(List.of(regra, regra))).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new Regra("camadas-1", "x", Categoria.CAMADAS, Severidade.ERRO, c -> Optional.empty()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ResultadoRegra resultado(List<ResultadoRegra> resultados, String id) {
        return resultados.stream().filter(r -> r.regra().id().equals(id)).findFirst().orElseThrow();
    }

    /** Provedor registrado em META-INF/services dos testes, simulando uma regra alternativa. */
    public static final class ProvedorDeTeste implements ProvedorDeRegras {

        @Override
        public List<Regra> regras() {
            return List.of(new Regra("EXTRA-001", "Regra alternativa de exemplo", Categoria.CODIGO, Severidade.AVISO, c -> Optional.empty()));
        }
    }
}

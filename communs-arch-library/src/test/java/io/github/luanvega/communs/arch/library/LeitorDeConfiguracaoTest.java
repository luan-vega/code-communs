package io.github.luanvega.communs.arch.library;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class LeitorDeConfiguracaoTest {

    @Test
    void leTodasAsChaves() {
        ConfiguracaoRegras config = ler("""
                pacoteRaiz: com.exemplo.pedidos
                excecaoBase: com.exemplo.pedidos.domain.ErroDeNegocio
                camadas:
                  controller: "..web.."
                  service: "..aplicacao.."
                  repository: "..persistencia.."
                  dominio: "..dominio.."
                  infra: ["..adapters..", "..infra.."]
                desligar: [NOMES-003, CODIGO-001]
                falharEmAvisos: true
                """);

        assertThat(config.pacoteRaiz()).contains("com.exemplo.pedidos");
        assertThat(config.excecaoBase()).contains("com.exemplo.pedidos.domain.ErroDeNegocio");
        assertThat(config.pacoteController()).isEqualTo("..web..");
        assertThat(config.pacoteService()).isEqualTo("..aplicacao..");
        assertThat(config.pacoteRepository()).isEqualTo("..persistencia..");
        assertThat(config.pacoteDominio()).isEqualTo("..dominio..");
        assertThat(config.pacotesInfra()).containsExactlyInAnyOrder("..adapters..", "..infra..");
        assertThat(config.regrasDesligadas()).containsExactlyInAnyOrder("NOMES-003", "CODIGO-001");
        assertThat(config.falharEmAvisos()).isTrue();
    }

    @Test
    void arquivoVazioOuParcialMantemPadroes() {
        assertThat(ler("").pacoteController()).isEqualTo("..controller..");

        ConfiguracaoRegras parcial = ler("desligar: NOMES-001\n");
        assertThat(parcial.estaDesligada("NOMES-001")).isTrue();
        assertThat(parcial.pacoteDominio()).isEqualTo("..domain..");
        assertThat(parcial.falharEmAvisos()).isFalse();
    }

    @Test
    void recusaChavesDesconhecidasETiposErrados() {
        assertThatThrownBy(() -> ler("pacoteRaz: com.exemplo\n"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("chave desconhecida 'pacoteRaz'");
        assertThatThrownBy(() -> ler("camadas:\n  dominios: x\n"))
                .hasMessageContaining("chave desconhecida 'camadas.dominios'");
        assertThatThrownBy(() -> ler("falharEmAvisos: sim\n")).hasMessageContaining("true ou false");
        assertThatThrownBy(() -> ler("desligar: {a: 1}\n")).hasMessageContaining("lista de textos");
    }

    @Test
    void naoInstanciaClassesArbitrarias() {
        assertThatThrownBy(() -> ler("!!java.io.File [\"/tmp\"]\n")).isInstanceOf(RuntimeException.class);
    }

    private static ConfiguracaoRegras ler(String yaml) {
        return LeitorDeConfiguracao.ler(new ByteArrayInputStream(yaml.getBytes(StandardCharsets.UTF_8)), "teste.yaml");
    }
}

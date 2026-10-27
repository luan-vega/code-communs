package io.github.luanvega.communs.arch.library;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

/**
 * Lê a {@link ConfiguracaoRegras} de um arquivo {@code regras.yaml}. Campos ausentes mantêm o valor padrão.
 *
 * <pre>{@code
 * pacoteRaiz: com.exemplo.pedidos
 * excecaoBase: com.exemplo.pedidos.domain.ErroDeNegocio
 * camadas:
 *   controller: "..controller.."
 *   service: "..service.."
 *   repository: "..repository.."
 *   dominio: "..domain.."
 *   infra: ["..infra..", "..adapters.."]
 * desligar: [NOMES-003]
 * falharEmAvisos: false
 * }</pre>
 */
public final class LeitorDeConfiguracao {

    public static final String NOME_PADRAO = "regras.yaml";

    private static final Set<String> CHAVES = Set.of("pacoteRaiz", "excecaoBase", "camadas", "desligar", "falharEmAvisos");
    private static final Set<String> CHAVES_CAMADAS = Set.of("controller", "service", "repository", "dominio", "infra");

    private LeitorDeConfiguracao() {
    }

    public static ConfiguracaoRegras ler(Path arquivo) {
        try (InputStream in = Files.newInputStream(arquivo)) {
            return ler(in, arquivo.toString());
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + arquivo, e);
        }
    }

    public static ConfiguracaoRegras ler(InputStream yaml, String origem) {
        // SafeConstructor: só tipos básicos do YAML, nunca instancia classes arbitrárias.
        Object raiz = new Yaml(new SafeConstructor(new LoaderOptions())).load(yaml);
        ConfiguracaoRegras.Builder builder = ConfiguracaoRegras.builder();
        if (raiz == null) {
            return builder.build();
        }
        Map<String, Object> mapa = mapa(raiz, origem, "raiz");
        validarChaves(mapa, CHAVES, origem, "");
        texto(mapa, "pacoteRaiz", origem).ifPresent(builder::pacoteRaiz);
        texto(mapa, "excecaoBase", origem).ifPresent(builder::excecaoBase);
        if (mapa.containsKey("camadas")) {
            Map<String, Object> camadas = mapa(mapa.get("camadas"), origem, "camadas");
            validarChaves(camadas, CHAVES_CAMADAS, origem, "camadas.");
            texto(camadas, "controller", origem).ifPresent(builder::pacoteController);
            texto(camadas, "service", origem).ifPresent(builder::pacoteService);
            texto(camadas, "repository", origem).ifPresent(builder::pacoteRepository);
            texto(camadas, "dominio", origem).ifPresent(builder::pacoteDominio);
            if (camadas.containsKey("infra")) {
                builder.pacotesInfra(lista(camadas.get("infra"), origem, "camadas.infra"));
            }
        }
        if (mapa.containsKey("desligar")) {
            builder.desligar(lista(mapa.get("desligar"), origem, "desligar").toArray(String[]::new));
        }
        if (mapa.containsKey("falharEmAvisos")) {
            Object valor = mapa.get("falharEmAvisos");
            if (!(valor instanceof Boolean falhar)) {
                throw erro(origem, "'falharEmAvisos' deve ser true ou false");
            }
            builder.falharEmAvisos(falhar);
        }
        return builder.build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mapa(Object valor, String origem, String campo) {
        if (!(valor instanceof Map<?, ?> mapa)) {
            throw erro(origem, "'" + campo + "' deve ser um mapa de chave: valor");
        }
        return (Map<String, Object>) mapa;
    }

    private static java.util.Optional<String> texto(Map<String, Object> mapa, String chave, String origem) {
        Object valor = mapa.get(chave);
        if (valor == null) {
            return java.util.Optional.empty();
        }
        if (!(valor instanceof String texto) || texto.isBlank()) {
            throw erro(origem, "'" + chave + "' deve ser um texto");
        }
        return java.util.Optional.of(texto.strip());
    }

    private static Set<String> lista(Object valor, String origem, String campo) {
        Collection<?> itens = valor instanceof Collection<?> colecao ? colecao : valor instanceof String texto ? List.of(texto) : null;
        if (itens == null || itens.stream().anyMatch(item -> !(item instanceof String))) {
            throw erro(origem, "'" + campo + "' deve ser uma lista de textos");
        }
        Set<String> textos = new LinkedHashSet<>();
        itens.forEach(item -> textos.add(((String) item).strip()));
        return textos;
    }

    private static void validarChaves(Map<String, Object> mapa, Set<String> validas, String origem, String prefixo) {
        for (String chave : mapa.keySet()) {
            if (!validas.contains(chave)) {
                throw erro(origem, "chave desconhecida '" + prefixo + chave + "'. Chaves aceitas: " + validas.stream().sorted().toList());
            }
        }
    }

    private static IllegalArgumentException erro(String origem, String mensagem) {
        return new IllegalArgumentException("Configuração inválida em " + origem + ": " + mensagem);
    }
}

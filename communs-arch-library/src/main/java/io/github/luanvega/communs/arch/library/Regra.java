package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.lang.ArchRule;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Regra do catálogo: id estável, nome, categoria, severidade e a fábrica que monta a {@link ArchRule}
 * a partir da {@link ConfiguracaoRegras}. A fábrica devolve vazio quando a regra precisa de uma
 * configuração que não foi informada.
 *
 * <p>A documentação de cada regra fica em {@code /regras/<ID>.md} no classpath, para relatórios,
 * documentação e o futuro chat usarem o mesmo texto.
 */
public record Regra(
        String id,
        String nome,
        Categoria categoria,
        Severidade severidade,
        Function<ConfiguracaoRegras, Optional<ArchRule>> fabrica) {

    private static final Pattern ID = Pattern.compile("[A-Z]+-\\d{3}");

    public Regra {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(categoria, "categoria");
        Objects.requireNonNull(severidade, "severidade");
        Objects.requireNonNull(fabrica, "fabrica");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Id de regra deve seguir CATEGORIA-000: " + id);
        }
    }

    public Optional<ArchRule> criar(ConfiguracaoRegras configuracao) {
        return fabrica.apply(configuracao).map(regra -> regra.as("[" + id + "] " + regra.description()));
    }

    /**
     * Texto em Markdown que explica a regra, ou vazio se a regra não tiver documentação.
     */
    public Optional<String> documentacao() {
        try (InputStream in = Regra.class.getResourceAsStream("/regras/" + id + ".md")) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a documentação de " + id, e);
        }
    }
}

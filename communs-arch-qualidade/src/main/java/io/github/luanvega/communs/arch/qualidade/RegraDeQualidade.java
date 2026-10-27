package io.github.luanvega.communs.arch.qualidade;

import io.github.luanvega.communs.arch.library.Severidade;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * Regra de qualidade: olha a configuração do projeto e devolve uma mensagem por problema encontrado.
 * A documentação fica em {@code /qualidade/<ID>.md} no classpath.
 */
public record RegraDeQualidade(String id, String nome, Severidade severidade, Function<ProjetoMaven, List<String>> verificacao) {

    private static final Pattern ID = Pattern.compile("QUALIDADE-\\d{3}");

    public RegraDeQualidade {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(nome, "nome");
        Objects.requireNonNull(severidade, "severidade");
        Objects.requireNonNull(verificacao, "verificacao");
        if (!ID.matcher(id).matches()) {
            throw new IllegalArgumentException("Id de regra de qualidade deve seguir QUALIDADE-000: " + id);
        }
    }

    public List<String> verificar(ProjetoMaven projeto) {
        return List.copyOf(verificacao.apply(projeto));
    }

    public Optional<String> documentacao() {
        try (InputStream in = RegraDeQualidade.class.getResourceAsStream("/qualidade/" + id + ".md")) {
            return in == null ? Optional.empty() : Optional.of(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler a documentação de " + id, e);
        }
    }
}

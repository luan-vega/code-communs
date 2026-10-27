package io.github.luanvega.communs.arch.qualidade;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Resultado das regras de qualidade sobre um projeto.
 */
public record VerificacaoDeQualidade(ProjetoMaven projeto, List<ResultadoQualidade> resultados) {

    public VerificacaoDeQualidade {
        resultados = List.copyOf(resultados);
    }

    public boolean falhou(boolean falharEmAvisos) {
        return resultados.stream().anyMatch(resultado -> resultado.deveFalhar(falharEmAvisos));
    }

    public int totalDeViolacoes() {
        return resultados.stream().mapToInt(resultado -> resultado.violacoes().size()).sum();
    }

    /**
     * Lança {@link AssertionError} listando as violações que devem quebrar o build. Útil num teste comum do JUnit.
     */
    public void exigirSemFalhas(boolean falharEmAvisos) {
        if (falhou(falharEmAvisos)) {
            throw new AssertionError(resultados.stream()
                    .filter(resultado -> resultado.deveFalhar(falharEmAvisos))
                    .flatMap(resultado -> resultado.violacoes().stream().map(v -> "[" + resultado.regra().id() + "] " + v))
                    .collect(Collectors.joining(System.lineSeparator(), "Configuração de qualidade com problemas:" + System.lineSeparator(), "")));
        }
    }
}

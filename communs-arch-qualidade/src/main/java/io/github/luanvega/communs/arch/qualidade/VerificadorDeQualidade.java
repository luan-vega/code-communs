package io.github.luanvega.communs.arch.qualidade;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Lê o projeto de um diretório e avalia as regras {@code QUALIDADE-xxx}.
 *
 * <pre>{@code
 * VerificacaoDeQualidade verificacao = new VerificadorDeQualidade().verificar(Path.of("."));
 * verificacao.exigirSemFalhas(false);
 * }</pre>
 */
public final class VerificadorDeQualidade {

    private final List<RegraDeQualidade> regras;

    public VerificadorDeQualidade() {
        this(RegrasDeQualidade.padrao());
    }

    public VerificadorDeQualidade(List<RegraDeQualidade> regras) {
        this.regras = List.copyOf(regras);
    }

    public List<RegraDeQualidade> regras() {
        return regras;
    }

    public VerificacaoDeQualidade verificar(Path diretorio) {
        return verificar(diretorio, Set.of());
    }

    /**
     * @param desligadas ids de regras a ignorar (os mesmos de {@code desligar} no {@code regras.yaml})
     */
    public VerificacaoDeQualidade verificar(Path diretorio, Set<String> desligadas) {
        ProjetoMaven projeto = LeitorDeProjeto.ler(diretorio);
        List<ResultadoQualidade> resultados = new ArrayList<>();
        for (RegraDeQualidade regra : regras) {
            resultados.add(desligadas.contains(regra.id())
                    ? ResultadoQualidade.ignorada(regra, "desligada na configuração")
                    : ResultadoQualidade.avaliada(regra, regra.verificar(projeto)));
        }
        return new VerificacaoDeQualidade(projeto, resultados);
    }
}

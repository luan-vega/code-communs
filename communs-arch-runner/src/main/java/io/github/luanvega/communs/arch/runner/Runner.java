package io.github.luanvega.communs.arch.runner;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.core.importer.ImportOption;
import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.RegrasRegistry;
import io.github.luanvega.communs.arch.library.ResultadoRegra;
import io.github.luanvega.communs.arch.qualidade.VerificacaoDeQualidade;
import io.github.luanvega.communs.arch.qualidade.VerificadorDeQualidade;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Optional;

/**
 * Importa as classes de um projeto e avalia o catálogo de Regras e, se pedido, as regras de qualidade. É o que a CLI usa por baixo,
 * e pode ser chamado direto de outro código (ex.: um plugin ou o futuro chat).
 */
public final class Runner {

    private final RegrasRegistry registry;
    private final VerificadorDeQualidade verificadorDeQualidade;
    private final Clock relogio;

    public Runner() {
        this(RegrasRegistry.padrao(), new VerificadorDeQualidade(), Clock.systemUTC());
    }

    public Runner(RegrasRegistry registry, VerificadorDeQualidade verificadorDeQualidade, Clock relogio) {
        this.registry = registry;
        this.verificadorDeQualidade = verificadorDeQualidade;
        this.relogio = relogio;
    }

    public Relatorio executar(List<Path> caminhos, ConfiguracaoRegras configuracao, boolean incluirTestes) {
        return executar(caminhos, null, configuracao, incluirTestes);
    }

    /**
     * @param caminhos pastas de classes ou JARs para as Regras de arquitetura (pode ser vazio)
     * @param projeto  diretório com {@code pom.xml}/{@code sonar-project.properties} para as regras
     *                 {@code QUALIDADE-xxx}, ou {@code null} para não verificar
     */
    public Relatorio executar(List<Path> caminhos, Path projeto, ConfiguracaoRegras configuracao, boolean incluirTestes) {
        int classesAnalisadas = 0;
        List<ResultadoRegra> resultados = List.of();
        if (!caminhos.isEmpty()) {
            ClassFileImporter importador = new ClassFileImporter();
            if (!incluirTestes) {
                importador = importador.withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS);
            }
            JavaClasses classes = importador.importPaths(caminhos.toArray(Path[]::new));
            classesAnalisadas = classes.size();
            resultados = registry.avaliar(classes, configuracao);
        }
        Optional<VerificacaoDeQualidade> qualidade = Optional.ofNullable(projeto)
                .map(diretorio -> verificadorDeQualidade.verificar(diretorio, configuracao.regrasDesligadas()));
        return new Relatorio(classesAnalisadas, resultados, qualidade, configuracao, relogio.instant());
    }
}

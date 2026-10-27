package io.github.luanvega.communs.arch.runner;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.core.importer.ImportOption;
import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.RegrasRegistry;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;

/**
 * Importa as classes de um projeto e avalia o catálogo de Regras. É o que a CLI usa por baixo,
 * e pode ser chamado direto de outro código (ex.: um plugin ou o futuro chat).
 */
public final class Runner {

    private final RegrasRegistry registry;
    private final Clock relogio;

    public Runner() {
        this(RegrasRegistry.padrao(), Clock.systemUTC());
    }

    public Runner(RegrasRegistry registry, Clock relogio) {
        this.registry = registry;
        this.relogio = relogio;
    }

    public Relatorio executar(List<Path> caminhos, ConfiguracaoRegras configuracao, boolean incluirTestes) {
        ClassFileImporter importador = new ClassFileImporter();
        if (!incluirTestes) {
            importador = importador.withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS);
        }
        JavaClasses classes = importador.importPaths(caminhos.toArray(Path[]::new));
        return new Relatorio(classes.size(), registry.avaliar(classes, configuracao), configuracao, relogio.instant());
    }
}

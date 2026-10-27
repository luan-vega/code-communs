package io.github.luanvega.communs.arch.junit;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.core.importer.ImportOption;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.ArchRuleViolationError;
import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.LeitorDeConfiguracao;
import io.github.luanvega.communs.arch.library.ResultadoRegra;
import io.github.luanvega.communs.arch.library.RegrasRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.junit.platform.engine.EngineExecutionListener;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestExecutionResult;
import org.opentest4j.TestAbortedException;

/**
 * Executa os descritores: importa as classes uma vez por conjunto de pacotes e roda cada regra.
 * Violações de Regras do catálogo com severidade AVISO viram testes abortados (aparecem como "skipped"
 * com o relatório), a não ser que a configuração tenha {@code falharEmAvisos: true}.
 */
final class Executor {

    private static final Map<String, JavaClasses> CACHE = new ConcurrentHashMap<>();

    private final EngineExecutionListener ouvinte;

    Executor(EngineExecutionListener ouvinte) {
        this.ouvinte = ouvinte;
    }

    void executar(TestDescriptor raiz) {
        ouvinte.executionStarted(raiz);
        raiz.getChildren().forEach(filho -> executarClasse((DescritorDeClasse) filho));
        ouvinte.executionFinished(raiz, TestExecutionResult.successful());
    }

    private void executarClasse(DescritorDeClasse descritor) {
        ouvinte.executionStarted(descritor);
        JavaClasses classes;
        ConfiguracaoRegras configuracao;
        try {
            AnalisarClasses analisar = descritor.classe().getAnnotation(AnalisarClasses.class);
            classes = importar(descritor.classe(), analisar);
            configuracao = configuracao(descritor.classe(), analisar.configuracao());
        } catch (RuntimeException | Error e) {
            ouvinte.executionFinished(descritor, TestExecutionResult.failed(e));
            return;
        }
        Object[] instancia = new Object[1];
        for (TestDescriptor filho : descritor.getChildren()) {
            ouvinte.executionStarted(filho);
            ouvinte.executionFinished(filho, executarRegra(descritor.classe(), ((DescritorDeRegra) filho).alvo(), classes, configuracao, instancia));
        }
        ouvinte.executionFinished(descritor, TestExecutionResult.successful());
    }

    private static TestExecutionResult executarRegra(
            Class<?> classe, DescritorDeRegra.Alvo alvo, JavaClasses classes, ConfiguracaoRegras configuracao, Object[] instancia) {
        try {
            if (alvo.campo() != null) {
                if (!Modifier.isStatic(alvo.campo().getModifiers())) {
                    throw new IllegalStateException("Campo @RegraArch deve ser static: " + alvo.campo());
                }
                alvo.campo().setAccessible(true);
                if (!(alvo.campo().get(null) instanceof ArchRule regra)) {
                    throw new IllegalStateException("Campo @RegraArch deve ser do tipo ArchRule: " + alvo.campo());
                }
                regra.check(classes);
            } else if (alvo.metodo() != null) {
                invocar(classe, alvo.metodo(), classes, instancia);
            } else {
                return catalogo(alvo, classes, configuracao);
            }
            return TestExecutionResult.successful();
        } catch (InvocationTargetException e) {
            return TestExecutionResult.failed(e.getCause());
        } catch (Throwable e) {
            return TestExecutionResult.failed(e);
        }
    }

    private static TestExecutionResult catalogo(DescritorDeRegra.Alvo alvo, JavaClasses classes, ConfiguracaoRegras configuracao) {
        ResultadoRegra resultado = RegrasRegistry.de(List.of(alvo.regra())).avaliar(classes, configuracao).getFirst();
        if (resultado.foiIgnorada()) {
            return TestExecutionResult.aborted(new TestAbortedException(alvo.regra().id() + " ignorada: " + resultado.motivo().orElse("")));
        }
        if (!resultado.temViolacoes()) {
            return TestExecutionResult.successful();
        }
        ArchRuleViolationError erro = new ArchRuleViolationError(resultado.avaliacao());
        if (configuracao.deveFalhar(resultado)) {
            return TestExecutionResult.failed(erro);
        }
        return TestExecutionResult.aborted(new TestAbortedException("AVISO: " + erro.getMessage()));
    }

    private static void invocar(Class<?> classe, Method metodo, JavaClasses classes, Object[] instancia) throws Exception {
        Class<?>[] parametros = metodo.getParameterTypes();
        if (metodo.getReturnType() != void.class || parametros.length > 1
                || (parametros.length == 1 && parametros[0] != JavaClasses.class)) {
            throw new IllegalStateException("Método @RegraArch deve ser void e receber nenhum parâmetro ou JavaClasses: " + metodo);
        }
        Object alvo = null;
        if (!Modifier.isStatic(metodo.getModifiers())) {
            if (instancia[0] == null) {
                Constructor<?> construtor = classe.getDeclaredConstructor();
                construtor.setAccessible(true);
                instancia[0] = construtor.newInstance();
            }
            alvo = instancia[0];
        }
        metodo.setAccessible(true);
        if (parametros.length == 1) {
            metodo.invoke(alvo, classes);
        } else {
            metodo.invoke(alvo);
        }
    }

    private static JavaClasses importar(Class<?> classe, AnalisarClasses analisar) {
        Set<String> pacotes = new LinkedHashSet<>(Arrays.asList(analisar.pacotes()));
        Arrays.stream(analisar.pacotesDe()).map(Class::getPackageName).forEach(pacotes::add);
        if (pacotes.isEmpty()) {
            pacotes.add(classe.getPackageName());
        }
        String chave = pacotes + "|" + analisar.incluirTestes();
        return CACHE.computeIfAbsent(chave, k -> {
            ClassFileImporter importador = new ClassFileImporter();
            if (!analisar.incluirTestes()) {
                importador = importador.withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS);
            }
            return importador.importPackages(pacotes.toArray(String[]::new));
        });
    }

    private static ConfiguracaoRegras configuracao(Class<?> classe, String recurso) {
        String caminho = recurso.startsWith("/") ? recurso : "/" + recurso;
        try (InputStream in = classe.getResourceAsStream(caminho)) {
            return in == null ? ConfiguracaoRegras.padrao() : LeitorDeConfiguracao.ler(in, recurso);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + recurso, e);
        }
    }
}

package io.github.luanvega.communs.arch.lang.syntax;

import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.classes;
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.EvaluationResult;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.fixtures.app.Servico;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Critério de pronto da fase 3: as mesmas regras, no mesmo código, apontam as mesmas classes que o ArchUnit original.
 * O ArchUnit é usado só aqui, como referência.
 */
class ParidadeComArchUnitTest {

    private static final String APP = "io.github.luanvega.communs.arch.lang.fixtures.app";
    private static final Pattern CLASSE = Pattern.compile("<(" + Pattern.quote(APP) + "[\\w.$]*)");

    private static JavaClasses nossas;
    private static com.tngtech.archunit.core.domain.JavaClasses deles;

    @BeforeAll
    static void importar() {
        nossas = new ClassFileImporter().importPackages(APP);
        deles = new com.tngtech.archunit.core.importer.ClassFileImporter().importPackages(APP);
    }

    @Test
    void controllerSemAcessoAoRepositorio() {
        comparar(
                noClasses().that().resideInAPackage("..controller..").should().accessClassesThat().resideInAPackage("..repository.."),
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses().that().resideInAPackage("..controller..")
                        .should().accessClassesThat().resideInAPackage("..repository.."));
    }

    @Test
    void camadasPermitidasParaControllers() {
        comparar(
                classes().that().resideInAPackage("..controller..")
                        .should().onlyDependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..domain..", "java.."),
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..controller..")
                        .should().onlyDependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..domain..", "java.."));
    }

    @Test
    void repositorioSoAcessadoPorServicos() {
        comparar(
                classes().that().resideInAPackage("..repository..")
                        .should().onlyBeAccessedByClassesThat().resideInAnyPackage("..service..", "..repository.."),
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..repository..")
                        .should().onlyBeAccessed().byAnyPackage("..service..", "..repository.."));
    }

    @Test
    void servicosAnotados() {
        comparar(
                classes().that().resideInAPackage("..service..").should().beAnnotatedWith(Servico.class),
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..service..")
                        .should().beAnnotatedWith(Servico.class));
        comparar(
                classes().that().resideInAPackage("..controller..").should().beAnnotatedWith(Servico.class),
                com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes().that().resideInAPackage("..controller..")
                        .should().beAnnotatedWith(Servico.class));
    }

    private static void comparar(io.github.luanvega.communs.arch.lang.ArchRule nossa, ArchRule original) {
        EvaluationResult resultadoOriginal = original.evaluate(deles);
        Set<String> classesOriginal = new TreeSet<>();
        resultadoOriginal.getFailureReport().getDetails().forEach(detalhe -> classesOriginal.addAll(classesCitadas(detalhe)));

        Set<String> classesNossas = new TreeSet<>();
        nossa.evaluate(nossas).violations().stream().map(ConditionEvent::message).forEach(m -> classesNossas.addAll(classesCitadas(m)));

        assertThat(classesNossas).as("classes citadas nas violações").isEqualTo(classesOriginal);
        assertThat(nossa.evaluate(nossas).hasViolations()).isEqualTo(resultadoOriginal.hasViolation());
    }

    private static Set<String> classesCitadas(String mensagem) {
        Set<String> nomes = new TreeSet<>();
        Matcher matcher = CLASSE.matcher(mensagem);
        while (matcher.find()) {
            String nome = matcher.group(1);
            // ArchUnit cita membros (<a.B.metodo()>); fica só a classe.
            nomes.add(nome.replaceAll("\\.[a-z]\\w*$", ""));
        }
        return nomes;
    }
}

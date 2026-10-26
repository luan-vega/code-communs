package io.github.luanvega.communs.arch.lang.syntax;

import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.classes;
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.importer.ClassFileImporter;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.ArchRuleViolationError;
import io.github.luanvega.communs.arch.lang.ClassPredicates;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.EvaluationResult;
import io.github.luanvega.communs.arch.lang.fixtures.app.Injetar;
import io.github.luanvega.communs.arch.lang.fixtures.app.Servico;
import io.github.luanvega.communs.arch.lang.fixtures.app.repository.PedidoRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ArchRuleDefinitionTest {

    private static final String APP = "io.github.luanvega.communs.arch.lang.fixtures.app";

    private static JavaClasses classes;

    @BeforeAll
    static void importarApp() {
        classes = new ClassFileImporter().importPackages(APP);
    }

    @Test
    void controllerQueDependeDoRepositorioVioladaComLinha() {
        ArchRule regra = classes().that().resideInAPackage("..controller..")
                .should().onlyDependOnClassesThat().resideInAnyPackage("..controller..", "..service..", "..domain..", "java..");

        EvaluationResult resultado = regra.evaluate(classes);

        assertThat(regra.description()).isEqualTo(
                "classes that reside in a package '..controller..' should only depend on classes that "
                        + "reside in any package ['..controller..', '..service..', '..domain..', 'java..']");
        assertThat(resultado.violations()).extracting(ConditionEvent::message).contains(
                "Class <" + APP + ".controller.RelatorioController> depends on <" + APP + ".repository.PedidoRepository> "
                        + "[METHOD_CALL] in (RelatorioController.java:13)");
        assertThat(resultado.violations()).allMatch(v -> v.className().endsWith("RelatorioController"));
        // A anotação @Injetar fica no pacote raiz e também é uma dependência fora das camadas permitidas.
        assertThat(resultado.violations()).anyMatch(v -> v.message().contains(Injetar.class.getName()));
    }

    @Test
    void noClassesInverteACondicao() {
        ArchRule regra = noClasses().that().resideInAPackage("..controller..")
                .should().accessClassesThat().resideInAPackage("..repository..");

        EvaluationResult resultado = regra.evaluate(classes);

        assertThat(regra.description()).startsWith("no classes that reside in a package '..controller..' should access classes");
        assertThat(resultado.violations()).singleElement().satisfies(violacao -> {
            assertThat(violacao.className()).isEqualTo(APP + ".controller.RelatorioController");
            assertThat(violacao.lineNumber()).isEqualTo(13);
        });
    }

    @Test
    void accessClassesThatIgnoraDependenciasSoDeAssinatura() {
        // PedidoController só tem PedidoService como campo/parâmetro e chama service.buscar(): é acesso.
        // Já o tipo de retorno Pedido não é acesso, então essa regra passa.
        ArchRule regra = noClasses().that().haveSimpleName("PedidoController")
                .should().accessClassesThat().resideInAPackage("..domain..");

        assertThat(regra.evaluate(classes).hasViolations()).isFalse();
        assertThat(noClasses().that().haveSimpleName("PedidoController")
                .should().dependOnClassesThat().resideInAPackage("..domain..")
                .evaluate(classes).hasViolations()).isTrue();
    }

    @Test
    void regrasDeNomeEAnotacaoQuePassam() {
        assertThatCode(() -> classes().that().haveSimpleNameEndingWith("Service")
                .should().beAnnotatedWith(Servico.class)
                .andShould().resideInAPackage("..service..")
                .check(classes)).doesNotThrowAnyException();

        assertThatCode(() -> classes().that().areAnnotatedWith(Servico.class)
                .should().haveSimpleNameEndingWith("Service")
                .check(classes)).doesNotThrowAnyException();
    }

    @Test
    void proibeInjecaoPorCampo() {
        EvaluationResult resultado = noClasses().should().haveFieldsAnnotatedWith(Injetar.class).evaluate(classes);

        assertThat(resultado.violations()).extracting(ConditionEvent::message).containsExactly(
                "Field <" + APP + ".controller.RelatorioController.repository> is annotated with @Injetar in (RelatorioController.java:0)");
    }

    @Test
    void repositorioSoPodeSerAcessadoPorServicos() {
        EvaluationResult resultado = classes().that().resideInAPackage("..repository..")
                .should().onlyBeAccessedByClassesThat().resideInAnyPackage("..service..", "..repository..")
                .evaluate(classes);

        assertThat(resultado.violations()).extracting(ConditionEvent::className)
                .containsOnly(APP + ".controller.RelatorioController");
    }

    @Test
    void resolveHierarquiaImportadaEDoClasspath() {
        assertThatCode(() -> classes().that().areAssignableTo(PedidoRepository.class).and().areNotInterfaces()
                .should().haveSimpleNameStartingWith("PedidoRepository")
                .check(classes)).doesNotThrowAnyException();

        assertThatCode(() -> classes().that().areAssignableTo(Exception.class)
                .should().haveSimpleNameEndingWith("Exception")
                .andShould().resideInAPackage("..domain..")
                .check(classes)).doesNotThrowAnyException();
    }

    @Test
    void orShouldBastaUmaCondicao() {
        assertThatCode(() -> classes().that().resideInAnyPackage("..util..", "..repository..")
                .should().haveOnlyPrivateConstructors()
                .orShould().beInterfaces()
                .orShould().haveSimpleNameEndingWith("Jdbc")
                .check(classes)).doesNotThrowAnyException();

        assertThat(classes().that().resideInAPackage("..service..")
                .should().haveOnlyPrivateConstructors()
                .orShould().beInterfaces()
                .evaluate(classes).violations()).hasSize(2);
    }

    @Test
    void regraSemClassesFalhaPorPadrao() {
        ArchRule regra = classes().that().resideInAPackage("..nao.existe..").should().beInterfaces();

        assertThat(regra.evaluate(classes).violations()).singleElement()
                .extracting(ConditionEvent::message).asString().startsWith("Rule failed to check any classes");
        assertThat(regra.allowEmptyShould(true).evaluate(classes).hasViolations()).isFalse();
    }

    @Test
    void checkLancaErroComRelatorioEJustificativa() {
        ArchRule regra = noClasses().that().resideInAPackage("..controller..")
                .should().dependOnClassesThat().resideInAPackage("..repository..")
                .because("controllers falam só com services");

        assertThatThrownBy(() -> regra.check(classes))
                .isInstanceOf(ArchRuleViolationError.class)
                .hasMessageStartingWith("Architecture Violation - Rule 'no classes that reside in a package '..controller..' "
                        + "should depend on classes that reside in a package '..repository..', because controllers falam só com services' "
                        + "was violated (")
                .hasMessageContaining("RelatorioController.java:13");
    }

    @Test
    void aceitaPredicadosECondicoesProprios() {
        ArchCondition<io.github.luanvega.communs.arch.core.domain.JavaClass> semMetodosEstaticos =
                ArchCondition.from("not have static methods", (javaClass, events) -> javaClass.methods().stream()
                        .filter(m -> m.modifiers().contains(io.github.luanvega.communs.arch.core.domain.JavaModifier.STATIC))
                        .filter(m -> !m.isStaticInitializer())
                        .forEach(m -> events.add(ConditionEvent.violated(javaClass.name(), "Method " + m.name() + " is static"))));

        EvaluationResult resultado = classes()
                .that(ClassPredicates.resideInAPackage("..util..").or(ClassPredicates.resideInAPackage("..service..")))
                .should(semMetodosEstaticos)
                .as("utilitários não devem existir")
                .evaluate(classes);

        assertThat(resultado.ruleDescription()).isEqualTo("utilitários não devem existir");
        assertThat(resultado.violations()).extracting(ConditionEvent::message).containsExactly("Method limpar is static");
    }
}

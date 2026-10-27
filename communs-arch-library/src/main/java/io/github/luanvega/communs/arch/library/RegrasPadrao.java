package io.github.luanvega.communs.arch.library;

import static io.github.luanvega.communs.arch.lang.ClassPredicates.areAnnotatedWith;
import static io.github.luanvega.communs.arch.lang.ClassPredicates.areAssignableTo;
import static io.github.luanvega.communs.arch.lang.ClassPredicates.areInterfaces;
import static io.github.luanvega.communs.arch.lang.ClassPredicates.haveFullyQualifiedName;
import static io.github.luanvega.communs.arch.lang.ClassPredicates.resideInAPackage;
import static io.github.luanvega.communs.arch.lang.ClassPredicates.resideInAnyPackage;
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.classes;
import static io.github.luanvega.communs.arch.lang.syntax.ArchRuleDefinition.noClasses;

import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.DescribedPredicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * As 10 Regras padrão do catálogo. Cada uma tem a documentação em {@code /regras/<ID>.md}.
 */
public final class RegrasPadrao implements ProvedorDeRegras {

    @Override
    public List<Regra> regras() {
        return List.of(
                new Regra("CAMADAS-001", "Camadas seguem controller → service → repository", Categoria.CAMADAS, Severidade.ERRO,
                        config -> Optional.of(camadas(config))),
                new Regra("CAMADAS-002", "Domínio não depende de Spring nem de infraestrutura", Categoria.CAMADAS, Severidade.ERRO,
                        config -> Optional.of(dominioIsolado(config))),
                new Regra("CICLOS-001", "Sem ciclos entre pacotes de primeiro nível", Categoria.CICLOS, Severidade.ERRO,
                        config -> Optional.of(new SemCiclosEntrePacotes(config.pacoteRaiz().orElse(null)))),
                new Regra("NOMES-001", "Controllers REST terminam em Controller", Categoria.NOMES, Severidade.AVISO,
                        config -> Optional.of(classes().that(areAnnotatedWith(Anotacoes.REST_CONTROLLER))
                                .should().haveSimpleNameEndingWith("Controller")
                                .allowEmptyShould(true))),
                new Regra("NOMES-002", "Services terminam em Service", Categoria.NOMES, Severidade.AVISO,
                        config -> Optional.of(classes().that(areAnnotatedWith(Anotacoes.SERVICE))
                                .should().haveSimpleNameEndingWith("Service")
                                .allowEmptyShould(true))),
                new Regra("NOMES-003", "Interfaces de repositório terminam em Repository", Categoria.NOMES, Severidade.AVISO,
                        config -> Optional.of(classes()
                                .that(areInterfaces().and(areAssignableTo(Anotacoes.SPRING_DATA_REPOSITORY)
                                        .or(areAnnotatedWith(Anotacoes.REPOSITORY))))
                                .should().haveSimpleNameEndingWith("Repository")
                                .allowEmptyShould(true))),
                new Regra("SPRING-001", "Sem injeção por campo", Categoria.SPRING, Severidade.ERRO,
                        config -> Optional.of(noClasses()
                                .should().haveFieldsAnnotatedWith(Anotacoes.AUTOWIRED)
                                .orShould().haveFieldsAnnotatedWith(Anotacoes.JAKARTA_INJECT)
                                .orShould().haveFieldsAnnotatedWith(Anotacoes.JAVAX_INJECT)
                                .allowEmptyShould(true))),
                new Regra("SPRING-002", "Entidades JPA não saem pelo controller", Categoria.SPRING, Severidade.AVISO,
                        config -> Optional.of(classes().that(resideInAPackage(config.pacoteController())
                                        .or(areAnnotatedWith(Anotacoes.REST_CONTROLLER))
                                        .or(areAnnotatedWith(Anotacoes.CONTROLLER)))
                                .should(CondicoesDaBiblioteca.naoExporEntidades())
                                .allowEmptyShould(true))),
                new Regra("CODIGO-001", "Sem System.out, System.err nem printStackTrace", Categoria.CODIGO, Severidade.AVISO,
                        config -> Optional.of(classes()
                                .should(CondicoesDaBiblioteca.naoEscreverNoConsole())
                                .allowEmptyShould(true))),
                new Regra("CODIGO-002", "Exceções de negócio estendem a exceção base", Categoria.CODIGO, Severidade.AVISO,
                        config -> config.excecaoBase().map(base -> classes()
                                .that(resideInAPackage(config.pacoteDominio())
                                        .and(areAssignableTo(Exception.class))
                                        .and(DescribedPredicate.not(haveFullyQualifiedName(base))))
                                .should().beAssignableTo(base)
                                .allowEmptyShould(true))));
    }

    private static ArchRule camadas(ConfiguracaoRegras config) {
        String controller = config.pacoteController();
        String service = config.pacoteService();
        String repository = config.pacoteRepository();
        List<ArchRule> regras = new ArrayList<>();
        regras.add(noClasses().that(resideInAPackage(controller))
                .should().accessClassesThat(resideInAPackage(repository))
                .allowEmptyShould(true));
        regras.add(noClasses().that(resideInAPackage(service))
                .should().accessClassesThat(resideInAPackage(controller))
                .allowEmptyShould(true));
        regras.add(noClasses().that(resideInAPackage(repository))
                .should().accessClassesThat(resideInAnyPackage(controller, service))
                .allowEmptyShould(true));
        return new RegraComposta("layers should only be accessed in the order controller -> service -> repository", regras);
    }

    private static ArchRule dominioIsolado(ConfiguracaoRegras config) {
        List<String> proibidos = new ArrayList<>(List.of("org.springframework..", config.pacoteController(), config.pacoteRepository()));
        proibidos.addAll(config.pacotesInfra());
        return noClasses().that(resideInAPackage(config.pacoteDominio()))
                .should().dependOnClassesThat(resideInAnyPackage(proibidos.toArray(String[]::new)))
                .allowEmptyShould(true);
    }
}

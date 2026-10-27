package io.github.luanvega.communs.arch.qualidade;

import io.github.luanvega.communs.arch.library.Severidade;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo das regras {@code QUALIDADE-xxx}.
 */
public final class RegrasDeQualidade {

    static final String JACOCO = "org.jacoco:jacoco-maven-plugin";
    static final String PITEST = "org.pitest:pitest-maven";
    static final String PITEST_JUNIT5 = "org.pitest:pitest-junit5-plugin";

    private static final Set<String> CREDENCIAIS = Set.of("sonar.token", "sonar.login", "sonar.password");
    private static final Set<String> EXCLUI_TUDO = Set.of(
            "**", "**/*", "**/*.java", "src/**", "src/main/**", "src/main/java/**", "**/src/main/**", "**/src/main/java/**");
    private static final List<String> CAMADAS_COM_LOGICA = List.of("service", "domain", "dominio", "controller", "usecase", "application");

    private RegrasDeQualidade() {
    }

    public static List<RegraDeQualidade> padrao() {
        return List.of(
                new RegraDeQualidade("QUALIDADE-001", "Sem credenciais do Sonar em arquivos", Severidade.ERRO,
                        RegrasDeQualidade::semCredenciais),
                new RegraDeQualidade("QUALIDADE-002", "Cobertura do JaCoCo chega ao Sonar", Severidade.ERRO,
                        RegrasDeQualidade::coberturaGerada),
                new RegraDeQualidade("QUALIDADE-003", "Exclusões não escondem o código de produção", Severidade.ERRO,
                        RegrasDeQualidade::exclusoesNaoEscondemTudo),
                new RegraDeQualidade("QUALIDADE-004", "Cobertura não exclui camadas com regra de negócio", Severidade.AVISO,
                        RegrasDeQualidade::coberturaNaoExcluiCamadas),
                new RegraDeQualidade("QUALIDADE-005", "Pipeline espera o Quality Gate", Severidade.AVISO,
                        RegrasDeQualidade::esperaQualityGate),
                new RegraDeQualidade("QUALIDADE-006", "Testes de mutação configurados", Severidade.AVISO,
                        RegrasDeQualidade::mutacaoConfigurada),
                new RegraDeQualidade("QUALIDADE-007", "SonarCloud com organização", Severidade.ERRO,
                        RegrasDeQualidade::sonarCloudComOrganizacao));
    }

    private static List<String> semCredenciais(ProjetoMaven projeto) {
        List<String> violacoes = new ArrayList<>();
        for (String chave : CREDENCIAIS) {
            Propriedade propriedade = projeto.propriedades().get(chave);
            if (propriedade != null && !propriedade.valor().isBlank() && !propriedade.valor().startsWith("${")) {
                violacoes.add(chave + " está com valor fixo em " + propriedade.origem()
                        + "; use a variável de ambiente SONAR_TOKEN no CI");
            }
        }
        return violacoes.stream().sorted().toList();
    }

    private static List<String> coberturaGerada(ProjetoMaven projeto) {
        if (projeto.herdaDoParentDeQualidade()) {
            return List.of();
        }
        List<PluginMaven> jacoco = plugins(projeto, JACOCO);
        if (jacoco.isEmpty()) {
            return List.of("jacoco-maven-plugin não está no pom.xml; sem ele o Sonar mostra 0% de cobertura");
        }
        boolean agente = jacoco.stream().anyMatch(p -> p.goals().contains("prepare-agent"));
        boolean relatorio = jacoco.stream().anyMatch(p -> p.goals().contains("report") || p.goals().contains("report-aggregate"));
        List<String> violacoes = new ArrayList<>();
        if (!agente) {
            violacoes.add("jacoco-maven-plugin sem o goal prepare-agent; os testes rodam sem medir cobertura");
        }
        if (!relatorio) {
            violacoes.add("jacoco-maven-plugin sem o goal report; o jacoco.xml que o Sonar lê não é gerado");
        }
        return violacoes;
    }

    private static List<String> exclusoesNaoEscondemTudo(ProjetoMaven projeto) {
        List<String> violacoes = new ArrayList<>();
        for (String chave : List.of("sonar.exclusions", "sonar.coverage.exclusions")) {
            for (String padrao : padroes(projeto, chave)) {
                if (EXCLUI_TUDO.contains(padrao)) {
                    violacoes.add(chave + " contém '" + padrao + "', que tira todo o código de produção da análise");
                }
            }
        }
        return violacoes;
    }

    private static List<String> coberturaNaoExcluiCamadas(ProjetoMaven projeto) {
        List<String> violacoes = new ArrayList<>();
        for (String padrao : padroes(projeto, "sonar.coverage.exclusions")) {
            String minusculo = padrao.toLowerCase(Locale.ROOT);
            CAMADAS_COM_LOGICA.stream().filter(minusculo::contains).findFirst().ifPresent(camada ->
                    violacoes.add("sonar.coverage.exclusions contém '" + padrao + "', que tira a camada " + camada
                            + " da cobertura; exclua só código sem lógica (DTOs, configuração, classe main)"));
        }
        return violacoes;
    }

    private static List<String> esperaQualityGate(ProjetoMaven projeto) {
        if (projeto.herdaDoParentDeQualidade()) {
            return List.of();
        }
        return projeto.propriedade("sonar.qualitygate.wait").filter("true"::equalsIgnoreCase).isPresent()
                ? List.of()
                : List.of("sonar.qualitygate.wait não é true; o pipeline passa mesmo com o Quality Gate reprovado");
    }

    private static List<String> mutacaoConfigurada(ProjetoMaven projeto) {
        if (projeto.herdaDoParentDeQualidade()) {
            return List.of();
        }
        if (!projeto.temPom()) {
            return List.of();
        }
        List<PluginMaven> pitest = plugins(projeto, PITEST);
        if (pitest.isEmpty()) {
            return List.of("pitest-maven não está no pom.xml; os testes de mutação não rodam");
        }
        List<String> violacoes = new ArrayList<>();
        if (pitest.stream().noneMatch(p -> p.dependencias().contains(PITEST_JUNIT5))) {
            violacoes.add("pitest-maven sem a dependência pitest-junit5-plugin; o PIT não encontra testes do JUnit 5 e 6");
        }
        if (pitest.stream().noneMatch(p -> p.configuracao().containsKey("mutationThreshold"))) {
            violacoes.add("pitest-maven sem mutationThreshold; o build não falha quando a força dos testes cai");
        }
        return violacoes;
    }

    private static List<String> sonarCloudComOrganizacao(ProjetoMaven projeto) {
        boolean sonarCloud = projeto.propriedade("sonar.host.url").filter(url -> url.contains("sonarcloud.io")).isPresent();
        return sonarCloud && projeto.propriedade("sonar.organization").isEmpty()
                ? List.of("sonar.host.url aponta para o SonarCloud, mas falta sonar.organization")
                : List.of();
    }

    private static List<PluginMaven> plugins(ProjetoMaven projeto, String coordenada) {
        return projeto.plugins().stream().filter(p -> p.coordenada().equals(coordenada)).toList();
    }

    private static List<String> padroes(ProjetoMaven projeto, String chave) {
        Optional<String> valor = projeto.propriedade(chave);
        return valor.map(v -> List.of(v.split(","))).orElse(List.of()).stream()
                .map(String::trim)
                .filter(padrao -> !padrao.isEmpty())
                .toList();
    }
}

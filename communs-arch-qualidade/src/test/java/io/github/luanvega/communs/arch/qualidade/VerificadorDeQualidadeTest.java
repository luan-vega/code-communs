package io.github.luanvega.communs.arch.qualidade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.InstanceOfAssertFactories.LIST;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class VerificadorDeQualidadeTest {

    private static final String JACOCO = """
            <plugin>
                <groupId>org.jacoco</groupId>
                <artifactId>jacoco-maven-plugin</artifactId>
                <executions>
                    <execution><goals><goal>prepare-agent</goal></goals></execution>
                    <execution><id>report</id><goals><goal>report</goal></goals></execution>
                </executions>
            </plugin>
            """;

    private static final String PITEST = """
            <plugin>
                <groupId>org.pitest</groupId>
                <artifactId>pitest-maven</artifactId>
                <dependencies>
                    <dependency>
                        <groupId>org.pitest</groupId>
                        <artifactId>pitest-junit5-plugin</artifactId>
                    </dependency>
                </dependencies>
                <configuration>
                    <mutationThreshold>60</mutationThreshold>
                </configuration>
            </plugin>
            """;

    @TempDir
    Path projeto;

    private final VerificadorDeQualidade verificador = new VerificadorDeQualidade();

    @Test
    void projetoBemConfiguradoPassaEmTudo() throws IOException {
        pom("<sonar.qualitygate.wait>true</sonar.qualitygate.wait>", JACOCO + PITEST);

        VerificacaoDeQualidade verificacao = verificador.verificar(projeto);

        assertThat(violacoes(verificacao)).isEmpty();
        assertThat(verificacao.falhou(true)).isFalse();
        assertThat(verificacao.resultados()).hasSize(7);
    }

    @Test
    void parentDeQualidadeJaTrazJacocoPitEQualityGate() throws IOException {
        escrever("pom.xml", """
                <project>
                    <parent>
                        <groupId>io.github.luanvega.communs</groupId>
                        <artifactId>communs-qualidade-parent</artifactId>
                        <version>0.1.0</version>
                    </parent>
                    <artifactId>pedidos</artifactId>
                </project>
                """);

        assertThat(violacoes(verificador.verificar(projeto))).isEmpty();
    }

    @Test
    void credencialFixaEhErroMasVariavelNao() throws IOException {
        pom("""
                <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
                <sonar.login>${env.SONAR_LOGIN}</sonar.login>
                """, JACOCO + PITEST);
        escrever("sonar-project.properties", "sonar.token=squ_123\n");

        VerificacaoDeQualidade verificacao = verificador.verificar(projeto);

        assertThat(violacoes(verificacao)).containsOnlyKeys("QUALIDADE-001");
        assertThat(violacoes(verificacao).get("QUALIDADE-001")).singleElement().asString()
                .contains("sonar.token", "sonar-project.properties", "SONAR_TOKEN");
        assertThat(verificacao.falhou(false)).isTrue();
    }

    @Test
    void semJacocoOuSemReportACoberturaNaoChega() throws IOException {
        pom("<sonar.qualitygate.wait>true</sonar.qualitygate.wait>", PITEST);
        assertThat(violacoes(verificador.verificar(projeto)).get("QUALIDADE-002")).singleElement().asString()
                .contains("0% de cobertura");

        pom("<sonar.qualitygate.wait>true</sonar.qualitygate.wait>", PITEST + """
                <plugin>
                    <groupId>org.jacoco</groupId>
                    <artifactId>jacoco-maven-plugin</artifactId>
                    <executions><execution><goals><goal>prepare-agent</goal></goals></execution></executions>
                </plugin>
                """);
        assertThat(violacoes(verificador.verificar(projeto)).get("QUALIDADE-002")).singleElement().asString()
                .contains("sem o goal report");
    }

    @Test
    void exclusaoQuePegaTudoEhErro() throws IOException {
        pom("""
                <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
                <sonar.coverage.exclusions>**/dto/**, **/*.java</sonar.coverage.exclusions>
                """, JACOCO + PITEST);

        assertThat(violacoes(verificador.verificar(projeto)))
                .containsOnlyKeys("QUALIDADE-003")
                .extractingByKey("QUALIDADE-003").asInstanceOf(LIST)
                .singleElement().asString().contains("'**/*.java'");
    }

    @Test
    void excluirCamadaDeServicoDaCoberturaEhAviso() throws IOException {
        pom("""
                <sonar.qualitygate.wait>true</sonar.qualitygate.wait>
                <sonar.coverage.exclusions>**/dto/**,**/Service/**</sonar.coverage.exclusions>
                """, JACOCO + PITEST);

        VerificacaoDeQualidade verificacao = verificador.verificar(projeto);

        assertThat(violacoes(verificacao)).containsOnlyKeys("QUALIDADE-004");
        assertThat(verificacao.falhou(false)).isFalse();
        assertThat(verificacao.falhou(true)).isTrue();
    }

    @Test
    void semQualityGateWaitEhAviso() throws IOException {
        pom("<sonar.qualitygate.wait>false</sonar.qualitygate.wait>", JACOCO + PITEST);

        assertThat(violacoes(verificador.verificar(projeto))).containsOnlyKeys("QUALIDADE-005");
    }

    @Test
    void pitSemPluginDoJunitESemLimite() throws IOException {
        pom("<sonar.qualitygate.wait>true</sonar.qualitygate.wait>", JACOCO + """
                <plugin>
                    <groupId>org.pitest</groupId>
                    <artifactId>pitest-maven</artifactId>
                </plugin>
                """);

        assertThat(violacoes(verificador.verificar(projeto)).get("QUALIDADE-006"))
                .hasSize(2)
                .anySatisfy(v -> assertThat(v).contains("pitest-junit5-plugin"))
                .anySatisfy(v -> assertThat(v).contains("mutationThreshold"));
    }

    @Test
    void pitEmPerfilEPluginManagementConta() throws IOException {
        escrever("pom.xml", """
                <project>
                    <properties><sonar.qualitygate.wait>true</sonar.qualitygate.wait></properties>
                    <build>
                        <pluginManagement><plugins>%s</plugins></pluginManagement>
                    </build>
                    <profiles>
                        <profile>
                            <id>mutacao</id>
                            <build><plugins>%s</plugins></build>
                        </profile>
                    </profiles>
                </project>
                """.formatted(JACOCO, PITEST));

        assertThat(violacoes(verificador.verificar(projeto))).isEmpty();
    }

    @Test
    void sonarCloudSemOrganizacaoEhErro() throws IOException {
        pom("<sonar.qualitygate.wait>true</sonar.qualitygate.wait>", JACOCO + PITEST);
        escrever("sonar-project.properties", "sonar.host.url=https://sonarcloud.io\n");
        assertThat(violacoes(verificador.verificar(projeto))).containsOnlyKeys("QUALIDADE-007");

        escrever("sonar-project.properties", "sonar.host.url=https://sonarcloud.io\nsonar.organization=luan-vega\n");
        assertThat(violacoes(verificador.verificar(projeto))).isEmpty();
    }

    @Test
    void sonarProjectPropertiesValeSobreOPom() throws IOException {
        pom("<sonar.qualitygate.wait>false</sonar.qualitygate.wait>", JACOCO + PITEST);
        escrever("sonar-project.properties", "sonar.qualitygate.wait=true\n");

        ProjetoMaven lido = LeitorDeProjeto.ler(projeto);

        assertThat(lido.propriedades().get("sonar.qualitygate.wait"))
                .isEqualTo(new Propriedade("sonar.qualitygate.wait", "true", "sonar-project.properties"));
    }

    @Test
    void propriedadesDentroDaConfiguracaoDePluginNaoContam() throws IOException {
        pom("", JACOCO + PITEST + """
                <plugin>
                    <artifactId>maven-surefire-plugin</artifactId>
                    <configuration>
                        <properties><sonar.qualitygate.wait>true</sonar.qualitygate.wait></properties>
                    </configuration>
                </plugin>
                """);

        ProjetoMaven lido = LeitorDeProjeto.ler(projeto);

        assertThat(lido.propriedade("sonar.qualitygate.wait")).isEmpty();
        assertThat(lido.plugins("org.apache.maven.plugins", "maven-surefire-plugin")).hasSize(1);
    }

    @Test
    void projetoSemPomSoVerificaOSonarProperties() throws IOException {
        escrever("sonar-project.properties", "sonar.qualitygate.wait=true\n");

        assertThat(violacoes(verificador.verificar(projeto))).containsOnlyKeys("QUALIDADE-002");
    }

    @Test
    void pomComDoctypeEhRecusado() throws IOException {
        escrever("pom.xml", """
                <?xml version="1.0"?>
                <!DOCTYPE project [<!ENTITY x SYSTEM "file:///etc/passwd">]>
                <project><name>&x;</name></project>
                """);

        assertThatThrownBy(() -> verificador.verificar(projeto))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("pom.xml inválido");
    }

    @Test
    void regraDesligadaFicaIgnorada() throws IOException {
        pom("", JACOCO + PITEST);

        VerificacaoDeQualidade verificacao = verificador.verificar(projeto, Set.of("QUALIDADE-005"));

        assertThat(violacoes(verificacao)).isEmpty();
        assertThat(verificacao.resultados()).filteredOn(ResultadoQualidade::foiIgnorada).singleElement()
                .extracting(r -> r.regra().id()).isEqualTo("QUALIDADE-005");
    }

    @Test
    void exigirSemFalhasListaSoOQueQuebra() throws IOException {
        pom("<sonar.token>abc</sonar.token>", JACOCO + PITEST);

        VerificacaoDeQualidade verificacao = verificador.verificar(projeto);

        assertThatThrownBy(() -> verificacao.exigirSemFalhas(false))
                .isInstanceOf(AssertionError.class)
                .hasMessageContaining("[QUALIDADE-001] sonar.token")
                .hasMessageNotContaining("QUALIDADE-005");
    }

    @Test
    void diretorioInexistente() {
        assertThatThrownBy(() -> verificador.verificar(projeto.resolve("nao-existe")))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void todaRegraTemDocumentacao() {
        assertThat(RegrasDeQualidade.padrao()).allSatisfy(regra -> assertThat(regra.documentacao())
                .hasValueSatisfying(texto -> assertThat(texto).startsWith("# " + regra.id() + " " + regra.nome())));
    }

    private void pom(String propriedades, String plugins) throws IOException {
        escrever("pom.xml", """
                <project>
                    <groupId>com.exemplo</groupId>
                    <artifactId>pedidos</artifactId>
                    <properties>%s</properties>
                    <build><plugins>%s</plugins></build>
                </project>
                """.formatted(propriedades, plugins));
    }

    private void escrever(String arquivo, String conteudo) throws IOException {
        Files.writeString(projeto.resolve(arquivo), conteudo);
    }

    private static Map<String, List<String>> violacoes(VerificacaoDeQualidade verificacao) {
        return verificacao.resultados().stream()
                .filter(ResultadoQualidade::temViolacoes)
                .collect(Collectors.toMap(r -> r.regra().id(), ResultadoQualidade::violacoes));
    }
}

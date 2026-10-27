package io.github.luanvega.communs.arch.runner;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MainTest {

    /** Projeto de exemplo compilado na hora: um System.out (CODIGO-001, aviso) e, opcionalmente, injeção por campo (SPRING-001, erro). */
    private static final Map<String, String> PROJETO_COM_AVISO = Map.of(
            "com/exemplo/pedidos/service/PedidoService.java", """
                    package com.exemplo.pedidos.service;

                    public class PedidoService {
                        public void criar() {
                            System.out.println("criado");
                        }
                    }
                    """);

    private static final Map<String, String> PROJETO_COM_ERRO = Map.of(
            "org/springframework/beans/factory/annotation/Autowired.java", """
                    package org.springframework.beans.factory.annotation;

                    @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
                    public @interface Autowired {
                    }
                    """,
            "com/exemplo/pedidos/service/PedidoService.java", """
                    package com.exemplo.pedidos.service;

                    public class PedidoService {
                        @org.springframework.beans.factory.annotation.Autowired
                        private Object repositorio;
                    }
                    """);

    @TempDir
    static Path pasta;

    private static Path comAviso;
    private static Path comErro;

    private final ByteArrayOutputStream saida = new ByteArrayOutputStream();
    private final ByteArrayOutputStream erros = new ByteArrayOutputStream();

    @BeforeAll
    static void compilarProjetos() throws IOException {
        comAviso = compilar(pasta.resolve("com-aviso"), PROJETO_COM_AVISO);
        comErro = compilar(pasta.resolve("com-erro"), PROJETO_COM_ERRO);
    }

    @Test
    void avisoNaoFalhaPorPadrao() {
        int codigo = executar("--classes", comAviso.toString());

        assertThat(codigo).isEqualTo(Main.OK);
        assertThat(saida()).contains("AVISO CODIGO-001", "Resultado: OK (1 violações)");
    }

    @Test
    void falharEmAvisosTransformaAvisoEmFalha() {
        int codigo = executar("--classes", comAviso.toString(), "--falhar-em-avisos");

        assertThat(codigo).isEqualTo(Main.FALHOU);
        assertThat(saida()).contains("ERRO  CODIGO-001", "Resultado: FALHOU");
    }

    @Test
    void erroFalha() {
        int codigo = executar("--classes", comErro.toString());

        assertThat(codigo).isEqualTo(Main.FALHOU);
        assertThat(saida()).contains("ERRO  SPRING-001", "PedidoService");
    }

    @Test
    void jsonNaSaidaPadrao() {
        int codigo = executar("--classes", comAviso.toString(), "--json", "-");

        assertThat(codigo).isEqualTo(Main.OK);
        assertThat(saida())
                .startsWith("{")
                .contains("\"ferramenta\": \"communs-arch\"")
                .contains("\"falhou\": false")
                .contains("\"id\": \"CODIGO-001\"")
                .contains("\"status\": \"VIOLADA\"")
                .contains("\"classe\": \"com.exemplo.pedidos.service.PedidoService\"")
                .contains("\"linha\": 5")
                .contains("\"nome\": \"Sem inje\\u00e7\\u00e3o por campo\"")
                .doesNotContain("Resultado:");
    }

    @Test
    void jsonEmArquivo() throws IOException {
        Path relatorio = pasta.resolve("relatorio.json");

        int codigo = executar("--classes", comErro.toString(), "--json", relatorio.toString());

        assertThat(codigo).isEqualTo(Main.FALHOU);
        assertThat(saida()).contains("Resultado: FALHOU", "Relatório JSON: " + relatorio);
        assertThat(Files.readString(relatorio)).contains("\"id\": \"SPRING-001\"", "\"falhou\": true");
    }

    @Test
    void configuracaoDesligaRegra() throws IOException {
        Path config = pasta.resolve("regras.yaml");
        Files.writeString(config, """
                desligar: [SPRING-001]
                """);

        int codigo = executar("--classes", comErro.toString(), "--config", config.toString());

        assertThat(codigo).isEqualTo(Main.OK);
        assertThat(saida()).contains("--    SPRING-001 Sem injeção por campo (");
    }

    @Test
    void configuracaoInvalidaEhErroDeUso() throws IOException {
        Path config = pasta.resolve("invalida.yaml");
        Files.writeString(config, "desligar: {SPRING-001: legado}\n");

        assertThat(executar("--classes", comErro.toString(), "--config", config.toString())).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("lista de textos");
    }

    @Test
    void pastaSemClassesEhErroDeUso() throws IOException {
        Path vazia = Files.createDirectories(pasta.resolve("vazia"));

        assertThat(executar("--classes", vazia.toString())).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Nenhuma classe encontrada", "--incluir-testes");
    }

    @Test
    void qualidadeSozinhaVerificaOProjeto() throws IOException {
        Path projeto = projetoComToken();

        int codigo = executar("--qualidade", projeto.toString());

        assertThat(codigo).isEqualTo(Main.FALHOU);
        assertThat(saida())
                .doesNotContain("classes analisadas")
                .contains("Qualidade: " + projeto, "ERRO  QUALIDADE-001", "sonar.token", "AVISO QUALIDADE-005");
    }

    @Test
    void qualidadeEArquiteturaNoMesmoJson() throws IOException {
        Path projeto = projetoComToken();

        int codigo = executar("--classes", comAviso.toString(), "--qualidade", projeto.toString(), "--json", "-");

        assertThat(codigo).isEqualTo(Main.FALHOU);
        assertThat(saida())
                .contains("\"id\": \"CODIGO-001\"")
                .contains("\"qualidade\": {")
                .contains("\"id\": \"QUALIDADE-001\"")
                .contains("\"mensagem\": \"sonar.token est\\u00e1 com valor fixo em pom.xml");
    }

    @Test
    void regraDeQualidadeDesligadaNoRegrasYaml() throws IOException {
        Path projeto = projetoComToken();
        Path config = pasta.resolve("sem-token.yaml");
        Files.writeString(config, "desligar: [QUALIDADE-001, QUALIDADE-002]\n");

        int codigo = executar("--qualidade", projeto.toString(), "--config", config.toString());

        assertThat(codigo).isEqualTo(Main.OK);
        assertThat(saida()).contains("--    QUALIDADE-001", "--    QUALIDADE-002", "Resultado: OK");
    }

    @Test
    void pastaDoProjetoInexistenteEhErroDeUso() {
        assertThat(executar("--qualidade", pasta.resolve("nao-existe").toString())).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Pasta do projeto não existe");
    }

    @Test
    void semClassesEhErroDeUso() {
        int codigo = executar("--json", "-");

        assertThat(codigo).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Informe ao menos um --classes ou o --qualidade", "Uso:");
    }

    @Test
    void opcaoDesconhecidaEhErroDeUso() {
        assertThat(executar("--classes", comAviso.toString(), "--rapido")).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Opção desconhecida: --rapido");
    }

    @Test
    void caminhoInexistenteEhErroDeUso() {
        assertThat(executar("--classes", pasta.resolve("nao-existe").toString())).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Caminho não existe");
    }

    @Test
    void configInexistenteEhErroDeUso() {
        assertThat(executar("--classes", comAviso.toString(), "--config", "nao-existe.yaml")).isEqualTo(Main.ERRO_DE_USO);
        assertThat(erros()).contains("Arquivo de configuração não existe");
    }

    @Test
    void ajuda() {
        assertThat(executar("--ajuda")).isEqualTo(Main.OK);
        assertThat(saida()).contains("--falhar-em-avisos");
    }

    private static Path projetoComToken() throws IOException {
        Path projeto = Files.createDirectories(pasta.resolve("projeto-" + System.nanoTime()));
        Files.writeString(projeto.resolve("pom.xml"), """
                <project>
                    <properties><sonar.token>squ_123</sonar.token></properties>
                </project>
                """);
        return projeto;
    }

    private int executar(String... args) {
        return Main.executar(args, new PrintStream(saida, true, StandardCharsets.UTF_8), new PrintStream(erros, true, StandardCharsets.UTF_8));
    }

    private String saida() {
        return saida.toString(StandardCharsets.UTF_8);
    }

    private String erros() {
        return erros.toString(StandardCharsets.UTF_8);
    }

    private static Path compilar(Path destino, Map<String, String> fontes) throws IOException {
        Path codigo = destino.resolve("src");
        Path classes = destino.resolve("classes");
        Files.createDirectories(classes);
        List<String> argumentos = new ArrayList<>(List.of("-d", classes.toString()));
        for (Map.Entry<String, String> fonte : fontes.entrySet()) {
            Path arquivo = codigo.resolve(fonte.getKey());
            Files.createDirectories(arquivo.getParent());
            Files.writeString(arquivo, fonte.getValue());
            argumentos.add(arquivo.toString());
        }
        JavaCompiler compilador = ToolProvider.getSystemJavaCompiler();
        assertThat(compilador.run(null, null, null, argumentos.toArray(String[]::new))).isZero();
        return classes;
    }
}

package io.github.luanvega.communs.arch.runner;

import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.LeitorDeConfiguracao;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * CLI do catálogo de Regras.
 *
 * <pre>
 * java -jar communs-arch-runner-cli.jar --classes target/classes [--classes outro.jar] [--qualidade .]
 *      [--config regras.yaml] [--json relatorio.json | --json -] [--falhar-em-avisos] [--incluir-testes]
 * </pre>
 *
 * Códigos de saída: 0 sem falhas, 1 alguma regra falhou, 2 erro de uso ou de configuração.
 */
public final class Main {

    static final int OK = 0;
    static final int FALHOU = 1;
    static final int ERRO_DE_USO = 2;

    private static final String USO = """
            Uso: java -jar communs-arch-runner-cli.jar --classes <pasta|jar> [--qualidade <projeto>] [opções]

              --classes <caminho>    pasta de classes compiladas ou JAR (pode repetir)
              --qualidade <pasta>    verifica pom.xml/sonar-project.properties da pasta (regras QUALIDADE-xxx)
              --config <arquivo>     regras.yaml (padrão: ./regras.yaml, se existir)
              --json <arquivo|->     grava o relatório JSON no arquivo ou, com '-', na saída padrão
              --falhar-em-avisos     violações de AVISO também falham (o mesmo que falharEmAvisos: true)
              --incluir-testes       analisa também classes de teste (test-classes)
              --ajuda                mostra esta ajuda

            Saída: 0 sem falhas, 1 alguma regra falhou, 2 erro de uso ou de configuração.
            """;

    private Main() {
    }

    public static void main(String[] args) {
        System.exit(executar(args, System.out, System.err));
    }

    static int executar(String[] args, PrintStream saida, PrintStream erros) {
        List<Path> classes = new ArrayList<>();
        Path config = null;
        Path projeto = null;
        String json = null;
        boolean falharEmAvisos = false;
        boolean incluirTestes = false;
        try {
            for (int i = 0; i < args.length; i++) {
                switch (args[i]) {
                    case "--classes" -> classes.add(Path.of(valor(args, ++i, "--classes")));
                    case "--qualidade" -> projeto = Path.of(valor(args, ++i, "--qualidade"));
                    case "--config" -> config = Path.of(valor(args, ++i, "--config"));
                    case "--json" -> json = valor(args, ++i, "--json");
                    case "--falhar-em-avisos" -> falharEmAvisos = true;
                    case "--incluir-testes" -> incluirTestes = true;
                    case "--ajuda", "-h", "--help" -> {
                        saida.print(USO);
                        return OK;
                    }
                    default -> throw new IllegalArgumentException("Opção desconhecida: " + args[i]);
                }
            }
            if (classes.isEmpty() && projeto == null) {
                throw new IllegalArgumentException("Informe ao menos um --classes ou o --qualidade");
            }
            if (projeto != null && !Files.isDirectory(projeto)) {
                throw new IllegalArgumentException("Pasta do projeto não existe: " + projeto);
            }
            for (Path caminho : classes) {
                if (!Files.exists(caminho)) {
                    throw new IllegalArgumentException("Caminho não existe: " + caminho);
                }
            }
            ConfiguracaoRegras configuracao = configuracao(config);
            if (falharEmAvisos) {
                configuracao = configuracao.toBuilder().falharEmAvisos(true).build();
            }

            Relatorio relatorio = new Runner().executar(classes, projeto, configuracao, incluirTestes);
            if (!classes.isEmpty() && relatorio.classesAnalisadas() == 0) {
                throw new IllegalArgumentException("Nenhuma classe encontrada em " + classes
                        + (incluirTestes ? "" : " (classes de teste só entram com --incluir-testes)"));
            }

            if ("-".equals(json)) {
                saida.print(relatorio.json());
            } else {
                saida.print(relatorio.resumoEmTexto());
                if (json != null) {
                    Files.writeString(Path.of(json), relatorio.json(), StandardCharsets.UTF_8);
                    saida.println("Relatório JSON: " + json);
                }
            }
            return relatorio.falhou() ? FALHOU : OK;
        } catch (IllegalArgumentException e) {
            erros.println("Erro: " + e.getMessage());
            erros.println();
            erros.print(USO);
            return ERRO_DE_USO;
        } catch (IOException | UncheckedIOException e) {
            erros.println("Erro: " + e.getMessage());
            return ERRO_DE_USO;
        }
    }

    private static ConfiguracaoRegras configuracao(Path config) {
        if (config != null) {
            if (!Files.exists(config)) {
                throw new IllegalArgumentException("Arquivo de configuração não existe: " + config);
            }
            return LeitorDeConfiguracao.ler(config);
        }
        Path padrao = Path.of(LeitorDeConfiguracao.NOME_PADRAO);
        return Files.exists(padrao) ? LeitorDeConfiguracao.ler(padrao) : ConfiguracaoRegras.padrao();
    }

    private static String valor(String[] args, int indice, String opcao) {
        if (indice >= args.length || args[indice].startsWith("--")) {
            throw new IllegalArgumentException("Falta o valor de " + opcao);
        }
        return args[indice];
    }
}

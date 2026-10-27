package io.github.luanvega.communs.arch.qualidade;

/**
 * Propriedade de configuração e o arquivo de onde veio ({@code pom.xml} ou {@code sonar-project.properties}).
 */
public record Propriedade(String chave, String valor, String origem) {
}

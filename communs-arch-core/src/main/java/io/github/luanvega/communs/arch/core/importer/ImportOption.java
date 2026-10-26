package io.github.luanvega.communs.arch.core.importer;

import java.util.regex.Pattern;

/**
 * Decide se um arquivo de classe encontrado deve ser importado.
 * A localização é um caminho com {@code /} como separador, inclusive dentro de JARs
 * (ex.: {@code /app/target/classes/com/exemplo/Pedido.class} ou {@code /libs/app.jar!/com/exemplo/Pedido.class}).
 */
@FunctionalInterface
public interface ImportOption {

    boolean includes(String location);

    /** Ignora classes de teste compiladas por Maven, Gradle ou IntelliJ. */
    ImportOption DO_NOT_INCLUDE_TESTS = new ImportOption() {
        private final Pattern testFolders = Pattern.compile(".*/(test-classes|classes/java/test|classes/kotlin/test|out/test)/.*");

        @Override
        public boolean includes(String location) {
            return !testFolders.matcher(location).matches();
        }
    };

    /** Ignora classes que estão dentro de arquivos JAR. */
    ImportOption DO_NOT_INCLUDE_JARS = location -> !location.contains(".jar!/");

    default ImportOption and(ImportOption other) {
        return location -> includes(location) && other.includes(location);
    }
}

package io.github.luanvega.communs.arch.core.importer;

import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

/**
 * Importa classes a partir de bytecode e devolve o modelo {@link JavaClasses}.
 *
 * <pre>{@code
 * JavaClasses classes = new ClassFileImporter()
 *         .withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS)
 *         .importPackages("com.exemplo.pedidos");
 * }</pre>
 *
 * Instâncias são imutáveis; {@link #withImportOption(ImportOption)} devolve um novo importador.
 */
public final class ClassFileImporter {

    private static final String CLASS_SUFFIX = ".class";

    private final ImportOption importOption;

    public ClassFileImporter() {
        this(location -> true);
    }

    private ClassFileImporter(ImportOption importOption) {
        this.importOption = importOption;
    }

    public ClassFileImporter withImportOption(ImportOption option) {
        return new ClassFileImporter(importOption.and(option));
    }

    /**
     * Importa todas as classes de pastas de classes compiladas e de arquivos JAR.
     */
    public JavaClasses importPaths(Path... paths) {
        List<JavaClass> classes = new ArrayList<>();
        for (Path path : paths) {
            if (Files.isDirectory(path)) {
                readDirectory(path, classes);
            } else if (path.toString().endsWith(".jar")) {
                readJar(path, "", classes);
            } else if (path.toString().endsWith(CLASS_SUFFIX)) {
                readFile(path, classes);
            } else {
                throw new IllegalArgumentException("Caminho não é pasta, JAR nem .class: " + path);
            }
        }
        return JavaClasses.of(classes);
    }

    /**
     * Importa as classes dos pacotes (e subpacotes) encontrados no classpath do class loader de contexto.
     */
    public JavaClasses importPackages(String... packageNames) {
        ClassLoader classLoader = classLoader();
        List<JavaClass> classes = new ArrayList<>();
        for (String packageName : packageNames) {
            String resourcePath = packageName.replace('.', '/');
            for (URL url : resources(classLoader, resourcePath)) {
                switch (url.getProtocol()) {
                    case "file" -> readDirectory(toPath(url), classes);
                    case "jar" -> readJar(jarPath(url), resourcePath + "/", classes);
                    default -> throw new IllegalArgumentException("Protocolo não suportado: " + url);
                }
            }
        }
        return JavaClasses.of(classes);
    }

    /**
     * Importa exatamente as classes informadas (sem classes aninhadas).
     */
    public JavaClasses importClasses(Class<?>... types) {
        List<JavaClass> classes = new ArrayList<>();
        for (Class<?> type : types) {
            String resource = type.getName().replace('.', '/') + CLASS_SUFFIX;
            ClassLoader classLoader = type.getClassLoader() != null ? type.getClassLoader() : ClassLoader.getPlatformClassLoader();
            URL url = classLoader.getResource(resource);
            if (url == null) {
                throw new IllegalArgumentException("Bytecode não encontrado para " + type.getName());
            }
            if (!importOption.includes(normalize(url.toString()))) {
                continue;
            }
            try (InputStream in = url.openStream()) {
                classes.add(JavaClassReader.read(in));
            } catch (IOException e) {
                throw new UncheckedIOException("Falha ao ler " + url, e);
            }
        }
        return JavaClasses.of(classes);
    }

    private void readDirectory(Path root, List<JavaClass> classes) {
        try (Stream<Path> files = Files.walk(root)) {
            files.filter(Files::isRegularFile)
                    .filter(file -> isImportableClassFile(file.getFileName().toString()))
                    .filter(file -> importOption.includes(normalize(file.toAbsolutePath().toString())))
                    .sorted()
                    .forEach(file -> readFile(file, classes));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao percorrer " + root, e);
        }
    }

    private void readFile(Path file, List<JavaClass> classes) {
        try (InputStream in = Files.newInputStream(file)) {
            classes.add(JavaClassReader.read(in));
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler " + file, e);
        }
    }

    private void readJar(Path jar, String entryPrefix, List<JavaClass> classes) {
        String jarLocation = normalize(jar.toAbsolutePath().toString()) + "!/";
        try (JarFile jarFile = new JarFile(jar.toFile())) {
            for (JarEntry entry : Collections.list(jarFile.entries())) {
                String entryName = entry.getName();
                if (entry.isDirectory()
                        || !entryName.startsWith(entryPrefix)
                        || entryName.startsWith("META-INF/")
                        || !isImportableClassFile(entryName.substring(entryName.lastIndexOf('/') + 1))
                        || !importOption.includes(jarLocation + entryName)) {
                    continue;
                }
                try (InputStream in = jarFile.getInputStream(entry)) {
                    classes.add(JavaClassReader.read(in));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao ler o JAR " + jar, e);
        }
    }

    private static boolean isImportableClassFile(String fileName) {
        return fileName.endsWith(CLASS_SUFFIX)
                && !fileName.equals("module-info.class")
                && !fileName.equals("package-info.class");
    }

    private static ClassLoader classLoader() {
        ClassLoader contextClassLoader = Thread.currentThread().getContextClassLoader();
        return contextClassLoader != null ? contextClassLoader : ClassFileImporter.class.getClassLoader();
    }

    private static List<URL> resources(ClassLoader classLoader, String resourcePath) {
        try {
            Enumeration<URL> urls = classLoader.getResources(resourcePath);
            return Collections.list(urls);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao procurar " + resourcePath + " no classpath", e);
        }
    }

    private static Path toPath(URL url) {
        try {
            return Path.of(url.toURI());
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("URL inválida: " + url, e);
        }
    }

    private static Path jarPath(URL url) {
        try {
            URL jarFileUrl = ((JarURLConnection) url.openConnection()).getJarFileURL();
            return toPath(jarFileUrl);
        } catch (IOException e) {
            throw new UncheckedIOException("Falha ao abrir " + url, e);
        }
    }

    private static String normalize(String location) {
        return location.replace('\\', '/');
    }
}

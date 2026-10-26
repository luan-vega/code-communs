package io.github.luanvega.communs.arch.core.importer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.luanvega.communs.arch.core.domain.Dependency;
import io.github.luanvega.communs.arch.core.domain.DependencyType;
import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClassKind;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.core.domain.JavaField;
import io.github.luanvega.communs.arch.core.domain.JavaMethod;
import io.github.luanvega.communs.arch.core.domain.JavaModifier;
import io.github.luanvega.communs.arch.core.fixtures.Marcador;
import io.github.luanvega.communs.arch.core.fixtures.Pedido;
import io.github.luanvega.communs.arch.core.fixtures.PedidoController;
import io.github.luanvega.communs.arch.core.fixtures.PedidoRepository;
import io.github.luanvega.communs.arch.core.fixtures.PedidoRepositoryEmMemoria;
import io.github.luanvega.communs.arch.core.fixtures.PedidoService;
import io.github.luanvega.communs.arch.core.fixtures.Status;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ClassFileImporterTest {

    private static final String FIXTURES = "io.github.luanvega.communs.arch.core.fixtures";

    private static JavaClasses classes;

    @BeforeAll
    static void importarFixtures() {
        classes = new ClassFileImporter().importPackages(FIXTURES);
    }

    @Test
    void importaTodasAsClassesDoPacoteIncluindoAninhadas() {
        assertThat(classes.stream().map(JavaClass::name)).containsExactlyInAnyOrder(
                Marcador.class.getName(),
                Pedido.class.getName(),
                Pedido.Item.class.getName(),
                PedidoController.class.getName(),
                PedidoRepository.class.getName(),
                PedidoRepositoryEmMemoria.class.getName(),
                PedidoService.class.getName(),
                Status.class.getName());
    }

    @Test
    void identificaOTipoDeDeclaracao() {
        assertThat(classe(Marcador.class).kind()).isEqualTo(JavaClassKind.ANNOTATION);
        assertThat(classe(PedidoRepository.class).kind()).isEqualTo(JavaClassKind.INTERFACE);
        assertThat(classe(Status.class).kind()).isEqualTo(JavaClassKind.ENUM);
        assertThat(classe(Pedido.class).kind()).isEqualTo(JavaClassKind.RECORD);
        assertThat(classe(PedidoService.class).kind()).isEqualTo(JavaClassKind.CLASS);
    }

    @Test
    void leNomesEModificadoresDeClassesAninhadas() {
        JavaClass item = classe(Pedido.Item.class);

        assertThat(item.simpleName()).isEqualTo("Item");
        assertThat(item.packageName()).isEqualTo(FIXTURES);
        assertThat(item.isNested()).isTrue();
        assertThat(item.modifiers()).contains(JavaModifier.PUBLIC, JavaModifier.STATIC, JavaModifier.FINAL);
        assertThat(item.sourceFileName()).contains("Pedido.java");
    }

    @Test
    void leHierarquiaEAnotacoesDaClasse() {
        JavaClass repositorio = classe(PedidoRepositoryEmMemoria.class);
        JavaClass service = classe(PedidoService.class);

        assertThat(repositorio.superclassName()).contains(Object.class.getName());
        assertThat(repositorio.interfaceNames()).containsExactly(PedidoRepository.class.getName());
        assertThat(service.isAnnotatedWith(Marcador.class)).isTrue();
        assertThat(repositorio.isAnnotatedWith(Marcador.class)).isFalse();
    }

    @Test
    void leCamposEMetodos() {
        JavaClass service = classe(PedidoService.class);

        assertThat(service.fields())
                .extracting(JavaField::name, JavaField::typeName)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("repository", PedidoRepository.class.getName()));
        assertThat(service.fields().getFirst().modifiers()).containsExactlyInAnyOrder(JavaModifier.PRIVATE, JavaModifier.FINAL);

        JavaMethod criar = metodo(service, "criar");
        assertThat(criar.returnTypeName()).isEqualTo(Pedido.class.getName());
        assertThat(criar.parameterTypeNames()).containsExactly(String.class.getName());
        assertThat(criar.throwsTypeNames()).containsExactly(IllegalStateException.class.getName());
        assertThat(criar.isAnnotatedWith(Marcador.class)).isTrue();

        assertThat(service.constructors()).singleElement()
                .extracting(JavaMethod::parameterTypeNames)
                .isEqualTo(List.of(PedidoRepository.class.getName()));
    }

    @Test
    void registraDependenciasDeAssinaturas() {
        JavaClass service = classe(PedidoService.class);

        assertThat(tipos(service, PedidoRepository.class.getName()))
                .contains(DependencyType.FIELD_TYPE, DependencyType.METHOD_PARAMETER, DependencyType.METHOD_CALL);
        assertThat(tipos(service, Marcador.class.getName())).containsOnly(DependencyType.ANNOTATION);
        assertThat(tipos(service, IllegalStateException.class.getName()))
                .contains(DependencyType.THROWS, DependencyType.CONSTRUCTOR_CALL);
        assertThat(tipos(classe(PedidoRepositoryEmMemoria.class), PedidoRepository.class.getName()))
                .containsOnly(DependencyType.IMPLEMENTS);
    }

    @Test
    void registraDependenciasDoCorpoDosMetodosComLinha() {
        JavaClass service = classe(PedidoService.class);

        assertThat(tipos(service, Pedido.class.getName()))
                .contains(DependencyType.METHOD_RETURN, DependencyType.CONSTRUCTOR_CALL, DependencyType.TYPE_REFERENCE);
        assertThat(tipos(service, BigDecimal.class.getName())).contains(DependencyType.FIELD_ACCESS);
        assertThat(tipos(service, Status.class.getName())).contains(DependencyType.FIELD_ACCESS);

        Dependency chamadaAoRepositorio = service.dependencies().stream()
                .filter(d -> d.targetClassName().equals(PedidoRepository.class.getName()))
                .filter(d -> d.type() == DependencyType.METHOD_CALL)
                .findFirst()
                .orElseThrow();
        assertThat(chamadaAoRepositorio.lineNumber()).isPositive();
        assertThat(chamadaAoRepositorio.describe()).contains("(linha ");
    }

    @Test
    void registraReferenciaDeMetodoComoChamada() {
        Dependency referencia = classe(PedidoService.class).dependencies().stream()
                .filter(d -> d.targetClassName().equals(Pedido.class.getName()))
                .filter(d -> d.type() == DependencyType.METHOD_CALL)
                .findFirst()
                .orElseThrow();

        assertThat(referencia.lineNumber()).isPositive();
    }

    @Test
    void naoRegistraDependenciaDaClasseParaElaMesma() {
        assertThat(classes).allSatisfy(javaClass ->
                assertThat(javaClass.dependsOn(javaClass.name())).isFalse());
    }

    @Test
    void importaDePastaDeClasses() throws Exception {
        JavaClasses daPasta = new ClassFileImporter().importPaths(pastaDasFixtures());

        assertThat(daPasta.size()).isEqualTo(classes.size());
        assertThat(daPasta.get(PedidoService.class).orElseThrow().dependencies())
                .isEqualTo(classe(PedidoService.class).dependencies());
    }

    @Test
    void importaDeJar(@TempDir Path temp) throws Exception {
        Path jar = criarJarComFixtures(temp.resolve("fixtures.jar"));

        JavaClasses doJar = new ClassFileImporter().importPaths(jar);

        assertThat(doJar.stream().map(JavaClass::name))
                .containsExactlyInAnyOrderElementsOf(classes.stream().map(JavaClass::name).toList());
    }

    @Test
    void importaClassesAvulsas() {
        JavaClasses avulsas = new ClassFileImporter().importClasses(PedidoService.class, String.class);

        assertThat(avulsas.contain(PedidoService.class)).isTrue();
        assertThat(avulsas.contain(String.class)).isTrue();
        assertThat(avulsas.contain(Pedido.class)).isFalse();
    }

    @Test
    void aplicaOpcoesDeImportacao(@TempDir Path temp) throws Exception {
        Path jar = criarJarComFixtures(temp.resolve("fixtures.jar"));

        assertThat(new ClassFileImporter().withImportOption(ImportOption.DO_NOT_INCLUDE_JARS).importPaths(jar).isEmpty()).isTrue();
        assertThat(new ClassFileImporter().withImportOption(ImportOption.DO_NOT_INCLUDE_TESTS).importPackages(FIXTURES).isEmpty()).isTrue();
        assertThat(new ClassFileImporter()
                .withImportOption(location -> !location.endsWith("Status.class"))
                .importPackages(FIXTURES)
                .contain(Status.class)).isFalse();
    }

    @Test
    void filtraClassesImportadas() {
        JavaClasses servicos = classes.that(javaClass -> javaClass.simpleName().endsWith("Service"));

        assertThat(servicos.stream().map(JavaClass::simpleName)).containsExactly("PedidoService");
    }

    @Test
    void recusaCaminhoQueNaoEhPastaNemJar(@TempDir Path temp) throws IOException {
        Path texto = Files.writeString(temp.resolve("leia-me.txt"), "nada");

        assertThatThrownBy(() -> new ClassFileImporter().importPaths(texto))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static JavaClass classe(Class<?> type) {
        return classes.get(type).orElseThrow();
    }

    private static JavaMethod metodo(JavaClass javaClass, String name) {
        return javaClass.methods().stream().filter(m -> m.name().equals(name)).findFirst().orElseThrow();
    }

    private static List<DependencyType> tipos(JavaClass origem, String alvo) {
        return origem.dependencies().stream()
                .filter(d -> d.targetClassName().equals(alvo))
                .map(Dependency::type)
                .toList();
    }

    private static Path pastaDasFixtures() throws Exception {
        Path raiz = Path.of(PedidoService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        return raiz.resolve(FIXTURES.replace('.', '/'));
    }

    private static Path criarJarComFixtures(Path jar) throws Exception {
        Path raiz = Path.of(PedidoService.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(jar));
                Stream<Path> arquivos = Files.walk(pastaDasFixtures())) {
            for (Path arquivo : arquivos.filter(Files::isRegularFile).toList()) {
                out.putNextEntry(new JarEntry(raiz.relativize(arquivo).toString().replace('\\', '/')));
                Files.copy(arquivo, (OutputStream) out);
                out.closeEntry();
            }
        }
        return jar;
    }
}

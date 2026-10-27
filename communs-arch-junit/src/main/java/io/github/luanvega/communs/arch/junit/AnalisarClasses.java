package io.github.luanvega.communs.arch.junit;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca uma classe de teste de arquitetura e diz quais classes importar.
 *
 * <pre>{@code
 * @AnalisarClasses(pacotes = "com.exemplo.pedidos", regrasPadrao = true)
 * class ArquiteturaTest {
 *
 *     @RegraArch
 *     static final ArchRule semCiclosNoDominio = noClasses().that().resideInAPackage("..domain..")
 *             .should().dependOnClassesThat().resideInAPackage("..controller..");
 * }
 * }</pre>
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AnalisarClasses {

    /**
     * Pacotes a importar (com subpacotes). Vazio junto com {@link #pacotesDe()} vazio = pacote da classe de teste.
     */
    String[] pacotes() default {};

    /**
     * Importa os pacotes destas classes, útil para não repetir nomes de pacote como texto.
     */
    Class<?>[] pacotesDe() default {};

    /**
     * Inclui classes compiladas de teste (ex.: {@code target/test-classes}). Padrão: não.
     */
    boolean incluirTestes() default false;

    /**
     * Roda também as Regras do catálogo ({@code RegrasRegistry.padrao()}), uma por teste.
     */
    boolean regrasPadrao() default false;

    /**
     * Recurso do classpath com a configuração das Regras. Se não existir, usa a configuração padrão.
     */
    String configuracao() default "regras.yaml";
}

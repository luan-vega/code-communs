package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.core.domain.Dependency;
import io.github.luanvega.communs.arch.core.domain.DependencyType;
import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaMethod;
import io.github.luanvega.communs.arch.core.domain.JavaModifier;
import io.github.luanvega.communs.arch.lang.ArchCondition;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Condições usadas pelas Regras padrão que não cabem nas condições genéricas da DSL.
 */
final class CondicoesDaBiblioteca {

    private CondicoesDaBiblioteca() {
    }

    /**
     * Violada quando a classe usa {@code System.out}, {@code System.err} ou chama {@code printStackTrace()}.
     */
    static ArchCondition<JavaClass> naoEscreverNoConsole() {
        return ArchCondition.from("not use System.out, System.err or printStackTrace()", (javaClass, events) -> {
            List<Dependency> usos = javaClass.dependencies().stream()
                    .filter(dependencia -> usaConsole(javaClass, dependencia))
                    .sorted(Comparator.comparingInt(Dependency::lineNumber))
                    .toList();
            if (usos.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(), "Class <" + javaClass.name() + "> does not write to the console"));
            }
            usos.forEach(uso -> events.add(new ConditionEvent(true,
                    "Class <" + javaClass.name() + "> uses <" + uso.target() + "> in (" + arquivo(javaClass) + ":" + uso.lineNumber() + ")",
                    javaClass.name(), uso.lineNumber())));
        });
    }

    private static boolean usaConsole(JavaClass origem, Dependency dependencia) {
        if (dependencia.type() == DependencyType.FIELD_ACCESS && dependencia.targetClassName().equals("java.lang.System")) {
            return dependencia.targetMemberName().equals("out") || dependencia.targetMemberName().equals("err");
        }
        return dependencia.type() == DependencyType.METHOD_CALL
                && dependencia.targetMemberName().equals("printStackTrace")
                && origem.resolve(dependencia.targetClassName()).isAssignableTo(Throwable.class);
    }

    /**
     * Violada quando um método não privado recebe ou devolve uma classe anotada como entidade JPA.
     * Tipos genéricos ({@code List<Pedido>}) ainda não são verificados.
     */
    static ArchCondition<JavaClass> naoExporEntidades() {
        return ArchCondition.from("not expose JPA entities in method signatures", (javaClass, events) -> {
            Set<String> encontradas = new LinkedHashSet<>();
            for (JavaMethod metodo : javaClass.methods()) {
                if (metodo.isConstructor() || metodo.isStaticInitializer()
                        || metodo.modifiers().contains(JavaModifier.PRIVATE) || metodo.modifiers().contains(JavaModifier.SYNTHETIC)) {
                    continue;
                }
                if (ehEntidade(javaClass, metodo.returnTypeName())) {
                    encontradas.add("Method <" + javaClass.name() + "." + metodo.name() + "> returns entity <" + metodo.returnTypeName() + ">");
                }
                for (String parametro : metodo.parameterTypeNames()) {
                    if (ehEntidade(javaClass, parametro)) {
                        encontradas.add("Method <" + javaClass.name() + "." + metodo.name() + "> receives entity <" + parametro + ">");
                    }
                }
            }
            if (encontradas.isEmpty()) {
                events.add(ConditionEvent.satisfied(javaClass.name(), "Class <" + javaClass.name() + "> does not expose JPA entities"));
            }
            encontradas.forEach(mensagem -> events.add(ConditionEvent.violated(javaClass.name(),
                    mensagem + " in (" + arquivo(javaClass) + ":0)")));
        });
    }

    private static boolean ehEntidade(JavaClass contexto, String nomeDoTipo) {
        String elemento = nomeDoTipo.replace("[]", "");
        if (elemento.indexOf('.') < 0) {
            return false;
        }
        JavaClass tipo = contexto.resolve(elemento);
        return Anotacoes.ENTITY.stream().anyMatch(tipo::isAnnotatedWith);
    }

    private static String arquivo(JavaClass javaClass) {
        return javaClass.sourceFileName().orElse(javaClass.simpleName() + ".java");
    }
}

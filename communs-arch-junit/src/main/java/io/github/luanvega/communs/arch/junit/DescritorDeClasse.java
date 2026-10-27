package io.github.luanvega.communs.arch.junit;

import io.github.luanvega.communs.arch.library.Regra;
import io.github.luanvega.communs.arch.library.RegrasRegistry;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Comparator;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;
import org.junit.platform.engine.support.descriptor.ClassSource;
import org.junit.platform.engine.support.descriptor.MethodSource;

/**
 * Container de uma classe com {@link AnalisarClasses}; os filhos são as regras a executar.
 */
final class DescritorDeClasse extends AbstractTestDescriptor {

    static final String TIPO = "class";

    private final Class<?> classe;

    private DescritorDeClasse(UniqueId id, Class<?> classe) {
        super(id, classe.getSimpleName(), ClassSource.from(classe));
        this.classe = classe;
    }

    static DescritorDeClasse criar(UniqueId pai, Class<?> classe) {
        DescritorDeClasse descritor = new DescritorDeClasse(pai.append(TIPO, classe.getName()), classe);
        UniqueId id = descritor.getUniqueId();
        Arrays.stream(classe.getDeclaredFields())
                .filter(campo -> campo.isAnnotationPresent(RegraArch.class))
                .sorted(Comparator.comparing(Field::getName))
                .forEach(campo -> descritor.addChild(new DescritorDeRegra(
                        id.append("field", campo.getName()), campo.getName(), ClassSource.from(classe), DescritorDeRegra.Alvo.campo(campo))));
        Arrays.stream(classe.getDeclaredMethods())
                .filter(metodo -> metodo.isAnnotationPresent(RegraArch.class))
                .sorted(Comparator.comparing(Method::getName))
                .forEach(metodo -> descritor.addChild(new DescritorDeRegra(
                        id.append("method", metodo.getName()), metodo.getName(), MethodSource.from(metodo), DescritorDeRegra.Alvo.metodo(metodo))));
        if (classe.getAnnotation(AnalisarClasses.class).regrasPadrao()) {
            for (Regra regra : RegrasRegistry.padrao().todas()) {
                descritor.addChild(new DescritorDeRegra(
                        id.append("regra", regra.id()), regra.id() + " " + regra.nome(), ClassSource.from(classe), DescritorDeRegra.Alvo.catalogo(regra)));
            }
        }
        return descritor;
    }

    Class<?> classe() {
        return classe;
    }

    @Override
    public Type getType() {
        return Type.CONTAINER;
    }
}

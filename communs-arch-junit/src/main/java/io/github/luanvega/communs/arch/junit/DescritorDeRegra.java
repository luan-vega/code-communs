package io.github.luanvega.communs.arch.junit;

import io.github.luanvega.communs.arch.library.Regra;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.platform.engine.TestSource;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.AbstractTestDescriptor;

/**
 * Um teste: um campo ou método {@link RegraArch}, ou uma Regra do catálogo.
 */
final class DescritorDeRegra extends AbstractTestDescriptor {

    private final Alvo alvo;

    DescritorDeRegra(UniqueId id, String nome, TestSource fonte, Alvo alvo) {
        super(id, nome, fonte);
        this.alvo = alvo;
    }

    Alvo alvo() {
        return alvo;
    }

    @Override
    public Type getType() {
        return Type.TEST;
    }

    /**
     * O que este teste executa. Só um dos campos é preenchido.
     */
    record Alvo(Field campo, Method metodo, Regra regra) {

        static Alvo campo(Field campo) {
            return new Alvo(campo, null, null);
        }

        static Alvo metodo(Method metodo) {
            return new Alvo(null, metodo, null);
        }

        static Alvo catalogo(Regra regra) {
            return new Alvo(null, null, regra);
        }
    }
}

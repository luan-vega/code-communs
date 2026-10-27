package io.github.luanvega.communs.arch.junit;

import java.util.Optional;
import org.junit.platform.engine.discovery.ClassSelector;
import org.junit.platform.engine.discovery.UniqueIdSelector;
import org.junit.platform.engine.support.discovery.SelectorResolver;

/**
 * Transforma cada classe com {@link AnalisarClasses} num container com um teste por regra.
 */
final class ResolvedorDeClasses implements SelectorResolver {

    @Override
    public Resolution resolve(ClassSelector selector, Context contexto) {
        Class<?> classe;
        try {
            classe = selector.getJavaClass();
        } catch (RuntimeException e) {
            return Resolution.unresolved();
        }
        if (!classe.isAnnotationPresent(AnalisarClasses.class)) {
            return Resolution.unresolved();
        }
        return contexto.addToParent(pai -> Optional.of(DescritorDeClasse.criar(pai.getUniqueId(), classe)))
                .map(descritor -> Resolution.match(Match.exact(descritor)))
                .orElse(Resolution.unresolved());
    }

    @Override
    public Resolution resolve(UniqueIdSelector selector, Context contexto) {
        // Reexecução pela IDE: o primeiro segmento depois da engine identifica a classe.
        return selector.getUniqueId().getSegments().stream()
                .filter(segmento -> segmento.getType().equals(DescritorDeClasse.TIPO))
                .findFirst()
                .flatMap(segmento -> carregar(segmento.getValue()))
                .map(classe -> resolve(org.junit.platform.engine.discovery.DiscoverySelectors.selectClass(classe), contexto))
                .orElse(Resolution.unresolved());
    }

    private static Optional<Class<?>> carregar(String nome) {
        try {
            return Optional.of(Class.forName(nome, false, Thread.currentThread().getContextClassLoader()));
        } catch (ClassNotFoundException e) {
            return Optional.empty();
        }
    }
}

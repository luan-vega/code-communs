package io.github.luanvega.communs.arch.junit;

import org.junit.platform.engine.EngineDiscoveryRequest;
import org.junit.platform.engine.ExecutionRequest;
import org.junit.platform.engine.TestDescriptor;
import org.junit.platform.engine.TestEngine;
import org.junit.platform.engine.UniqueId;
import org.junit.platform.engine.support.descriptor.EngineDescriptor;
import org.junit.platform.engine.support.discovery.EngineDiscoveryRequestResolver;

/**
 * Engine do JUnit Platform (id {@value #ID}) que encontra classes com {@link AnalisarClasses}
 * e roda cada {@link RegraArch} (e, se pedido, cada Regra do catálogo) como um teste.
 * É registrada por {@code META-INF/services/org.junit.platform.engine.TestEngine}; basta ter o JAR
 * no classpath de testes para o Maven Surefire e as IDEs a encontrarem.
 */
public final class CommunsArchTestEngine implements TestEngine {

    public static final String ID = "communs-arch";

    @Override
    public String getId() {
        return ID;
    }

    @Override
    public TestDescriptor discover(EngineDiscoveryRequest request, UniqueId uniqueId) {
        EngineDescriptor engine = new EngineDescriptor(uniqueId, "Communs Arch");
        EngineDiscoveryRequestResolver.<EngineDescriptor>builder()
                .addClassContainerSelectorResolver(classe -> classe.isAnnotationPresent(AnalisarClasses.class))
                .addSelectorResolver(contexto -> new ResolvedorDeClasses())
                .build()
                .resolve(request, engine);
        return engine;
    }

    @Override
    public void execute(ExecutionRequest request) {
        new Executor(request.getEngineExecutionListener()).executar(request.getRootTestDescriptor());
    }
}

package io.github.luanvega.communs.arch.qualidade;

import java.util.Map;
import java.util.Set;

/**
 * Plugin declarado no {@code pom.xml} (em {@code build}, {@code pluginManagement} ou num perfil).
 *
 * @param goals        goals das {@code executions}
 * @param dependencias dependências do plugin, como {@code groupId:artifactId}
 * @param configuracao elementos simples da {@code configuration} do plugin e das execuções
 */
public record PluginMaven(String groupId, String artifactId, Set<String> goals, Set<String> dependencias, Map<String, String> configuracao) {

    public PluginMaven {
        goals = Set.copyOf(goals);
        dependencias = Set.copyOf(dependencias);
        configuracao = Map.copyOf(configuracao);
    }

    public String coordenada() {
        return groupId + ":" + artifactId;
    }
}

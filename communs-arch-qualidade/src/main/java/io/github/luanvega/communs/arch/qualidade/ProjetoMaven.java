package io.github.luanvega.communs.arch.qualidade;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * O que as regras de qualidade enxergam de um projeto: propriedades {@code sonar.*} e afins,
 * plugins do build e o pom pai.
 *
 * @param pai          {@code groupId:artifactId} do pom pai, se houver
 * @param propriedades por chave; se a chave aparece no pom e no {@code sonar-project.properties}, vale a do segundo
 */
public record ProjetoMaven(Path diretorio, boolean temPom, Optional<String> pai, Map<String, Propriedade> propriedades, List<PluginMaven> plugins) {

    /** Pom pai da lib que já traz JaCoCo, PIT e Sonar configurados. */
    public static final String PARENT_DE_QUALIDADE = "io.github.luanvega.communs:communs-qualidade-parent";

    public ProjetoMaven {
        propriedades = Map.copyOf(propriedades);
        plugins = List.copyOf(plugins);
    }

    public Optional<String> propriedade(String chave) {
        return Optional.ofNullable(propriedades.get(chave)).map(Propriedade::valor).filter(valor -> !valor.isBlank());
    }

    public List<PluginMaven> plugins(String groupId, String artifactId) {
        return plugins.stream().filter(p -> p.groupId().equals(groupId) && p.artifactId().equals(artifactId)).toList();
    }

    public boolean herdaDoParentDeQualidade() {
        return pai.filter(PARENT_DE_QUALIDADE::equals).isPresent();
    }
}

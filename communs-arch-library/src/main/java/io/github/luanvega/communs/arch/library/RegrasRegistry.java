package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;

/**
 * Catálogo de Regras. {@link #padrao()} traz as Regras padrão e as de qualquer {@link ProvedorDeRegras}
 * registrado por {@link ServiceLoader}.
 */
public final class RegrasRegistry {

    private final Map<String, Regra> porId;

    private RegrasRegistry(List<Regra> regras) {
        Map<String, Regra> mapa = new LinkedHashMap<>();
        regras.stream().sorted(Comparator.comparing(Regra::id)).forEach(regra -> {
            if (mapa.putIfAbsent(regra.id(), regra) != null) {
                throw new IllegalStateException("Id de regra repetido: " + regra.id());
            }
        });
        this.porId = mapa;
    }

    public static RegrasRegistry padrao() {
        List<Regra> regras = new ArrayList<>(new RegrasPadrao().regras());
        ServiceLoader.load(ProvedorDeRegras.class).forEach(provedor -> regras.addAll(provedor.regras()));
        return new RegrasRegistry(regras);
    }

    public static RegrasRegistry de(List<Regra> regras) {
        return new RegrasRegistry(regras);
    }

    public List<Regra> todas() {
        return List.copyOf(porId.values());
    }

    public Optional<Regra> buscar(String id) {
        return Optional.ofNullable(porId.get(id));
    }

    /**
     * Avalia todas as Regras não desligadas na configuração, na ordem dos ids.
     */
    public List<ResultadoRegra> avaliar(JavaClasses classes, ConfiguracaoRegras configuracao) {
        List<ResultadoRegra> resultados = new ArrayList<>();
        for (Regra regra : porId.values()) {
            if (configuracao.estaDesligada(regra.id())) {
                resultados.add(ResultadoRegra.ignorada(regra, "desligada na configuração"));
                continue;
            }
            resultados.add(regra.criar(configuracao)
                    .map(archRule -> ResultadoRegra.avaliada(regra, archRule.evaluate(classes)))
                    .orElseGet(() -> ResultadoRegra.ignorada(regra, "falta configuração para esta regra")));
        }
        return resultados;
    }
}

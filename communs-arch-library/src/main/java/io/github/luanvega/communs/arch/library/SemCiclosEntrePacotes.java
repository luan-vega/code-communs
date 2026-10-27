package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.core.domain.Dependency;
import io.github.luanvega.communs.arch.core.domain.JavaClass;
import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.EvaluationResult;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Proíbe ciclos de dependência entre os subpacotes diretos do pacote raiz ("fatias"),
 * ex.: {@code com.app.pedido} usa {@code com.app.estoque} e {@code com.app.estoque} usa {@code com.app.pedido}.
 * Cada componente fortemente conexo do grafo de fatias com mais de uma fatia vira uma violação,
 * com um exemplo de dependência para cada aresta do ciclo.
 */
final class SemCiclosEntrePacotes implements ArchRule {

    private final String pacoteRaiz;
    private final String descricao;
    private final boolean permitirVazio;

    SemCiclosEntrePacotes(String pacoteRaiz) {
        this(pacoteRaiz, defaultDescription(pacoteRaiz), false);
    }

    private SemCiclosEntrePacotes(String pacoteRaiz, String descricao, boolean permitirVazio) {
        this.pacoteRaiz = pacoteRaiz;
        this.descricao = descricao;
        this.permitirVazio = permitirVazio;
    }

    private static String defaultDescription(String pacoteRaiz) {
        String raiz = pacoteRaiz == null ? "the common root package" : "'" + pacoteRaiz + "'";
        return "slices directly under " + raiz + " should be free of cycles";
    }

    @Override
    public String description() {
        return descricao;
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        List<ConditionEvent> violacoes = new ArrayList<>();
        if (classes.isEmpty()) {
            if (!permitirVazio) {
                violacoes.add(ConditionEvent.violated("", "Rule failed to check any classes. "
                        + "To allow rules being evaluated without checking any classes use allowEmptyShould(true)."));
            }
            return new EvaluationResult(descricao, violacoes);
        }
        String raiz = pacoteRaiz != null ? pacoteRaiz : prefixoComum(classes);

        // fatia de origem -> fatia de destino -> dependências que formam a aresta
        Map<String, Map<String, List<Dependency>>> grafo = new TreeMap<>();
        for (JavaClass origem : classes) {
            Optional<String> fatiaOrigem = fatia(raiz, origem.packageName());
            if (fatiaOrigem.isEmpty()) {
                continue;
            }
            grafo.computeIfAbsent(fatiaOrigem.get(), f -> new TreeMap<>());
            for (Dependency dependencia : origem.dependencies()) {
                classes.get(dependencia.targetClassName())
                        .flatMap(alvo -> fatia(raiz, alvo.packageName()))
                        .filter(fatiaAlvo -> !fatiaAlvo.equals(fatiaOrigem.get()))
                        .ifPresent(fatiaAlvo -> grafo.get(fatiaOrigem.get())
                                .computeIfAbsent(fatiaAlvo, f -> new ArrayList<>())
                                .add(dependencia));
            }
        }

        for (List<String> componente : componentesFortementeConexos(grafo)) {
            if (componente.size() > 1) {
                violacoes.add(violacao(raiz, componente, grafo));
            }
        }
        return new EvaluationResult(descricao, violacoes);
    }

    private static ConditionEvent violacao(String raiz, List<String> componente, Map<String, Map<String, List<Dependency>>> grafo) {
        TreeSet<String> fatias = new TreeSet<>(componente);
        StringBuilder mensagem = new StringBuilder("Cycle detected between slices " + fatias + " of '" + raiz + "':");
        String primeiraClasse = null;
        int primeiraLinha = 0;
        for (String origem : fatias) {
            for (Map.Entry<String, List<Dependency>> aresta : grafo.get(origem).entrySet()) {
                if (!fatias.contains(aresta.getKey())) {
                    continue;
                }
                Dependency exemplo = aresta.getValue().stream()
                        .min(Comparator.comparing(Dependency::originClassName).thenComparingInt(Dependency::lineNumber))
                        .orElseThrow();
                if (primeiraClasse == null) {
                    primeiraClasse = exemplo.originClassName();
                    primeiraLinha = exemplo.lineNumber();
                }
                mensagem.append(System.lineSeparator())
                        .append("  ").append(origem).append(" -> ").append(aresta.getKey())
                        .append(": <").append(exemplo.originClassName()).append("> depends on <").append(exemplo.target())
                        .append("> [").append(exemplo.type()).append("] (")
                        .append(aresta.getValue().size()).append(aresta.getValue().size() == 1 ? " dependency)" : " dependencies)");
            }
        }
        return new ConditionEvent(true, mensagem.toString(), primeiraClasse, primeiraLinha);
    }

    private static Optional<String> fatia(String raiz, String pacote) {
        if (raiz.isEmpty()) {
            return pacote.isEmpty() ? Optional.empty() : Optional.of(pacote.split("\\.")[0]);
        }
        if (!pacote.startsWith(raiz + ".")) {
            return Optional.empty();
        }
        return Optional.of(pacote.substring(raiz.length() + 1).split("\\.")[0]);
    }

    private static String prefixoComum(JavaClasses classes) {
        String[] prefixo = null;
        for (JavaClass javaClass : classes) {
            String[] partes = javaClass.packageName().isEmpty() ? new String[0] : javaClass.packageName().split("\\.");
            if (prefixo == null) {
                prefixo = partes;
                continue;
            }
            int iguais = 0;
            while (iguais < prefixo.length && iguais < partes.length && prefixo[iguais].equals(partes[iguais])) {
                iguais++;
            }
            prefixo = java.util.Arrays.copyOf(prefixo, iguais);
        }
        return prefixo == null ? "" : String.join(".", prefixo);
    }

    /**
     * Algoritmo de Tarjan.
     */
    private static List<List<String>> componentesFortementeConexos(Map<String, Map<String, List<Dependency>>> grafo) {
        Map<String, Integer> indice = new HashMap<>();
        Map<String, Integer> menor = new HashMap<>();
        java.util.Deque<String> pilha = new java.util.ArrayDeque<>();
        java.util.Set<String> naPilha = new java.util.HashSet<>();
        List<List<String>> componentes = new ArrayList<>();
        int[] contador = {0};
        for (String vertice : grafo.keySet()) {
            if (!indice.containsKey(vertice)) {
                visitar(vertice, grafo, indice, menor, pilha, naPilha, componentes, contador);
            }
        }
        return componentes;
    }

    private static void visitar(String vertice, Map<String, Map<String, List<Dependency>>> grafo, Map<String, Integer> indice,
            Map<String, Integer> menor, java.util.Deque<String> pilha, java.util.Set<String> naPilha,
            List<List<String>> componentes, int[] contador) {
        indice.put(vertice, contador[0]);
        menor.put(vertice, contador[0]);
        contador[0]++;
        pilha.push(vertice);
        naPilha.add(vertice);
        for (String vizinho : grafo.getOrDefault(vertice, Map.of()).keySet()) {
            if (!indice.containsKey(vizinho)) {
                visitar(vizinho, grafo, indice, menor, pilha, naPilha, componentes, contador);
                menor.put(vertice, Math.min(menor.get(vertice), menor.get(vizinho)));
            } else if (naPilha.contains(vizinho)) {
                menor.put(vertice, Math.min(menor.get(vertice), indice.get(vizinho)));
            }
        }
        if (menor.get(vertice).equals(indice.get(vertice))) {
            List<String> componente = new ArrayList<>();
            String atual;
            do {
                atual = pilha.pop();
                naPilha.remove(atual);
                componente.add(atual);
            } while (!atual.equals(vertice));
            componentes.add(componente);
        }
    }

    @Override
    public ArchRule because(String motivo) {
        return new SemCiclosEntrePacotes(pacoteRaiz, descricao + ", because " + motivo, permitirVazio);
    }

    @Override
    public ArchRule allowEmptyShould(boolean permitir) {
        return new SemCiclosEntrePacotes(pacoteRaiz, descricao, permitir);
    }

    @Override
    public ArchRule as(String novaDescricao) {
        return new SemCiclosEntrePacotes(pacoteRaiz, novaDescricao, permitirVazio);
    }
}

package io.github.luanvega.communs.arch.runner;

import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.ResultadoRegra;
import io.github.luanvega.communs.arch.qualidade.ResultadoQualidade;
import io.github.luanvega.communs.arch.qualidade.VerificacaoDeQualidade;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Resultado de uma execução do runner, com o JSON que outras ferramentas (CI, chat) consomem
 * e um resumo em texto para o terminal.
 */
public record Relatorio(
        int classesAnalisadas,
        List<ResultadoRegra> resultados,
        Optional<VerificacaoDeQualidade> qualidade,
        ConfiguracaoRegras configuracao,
        Instant geradoEm) {

    public Relatorio {
        resultados = List.copyOf(resultados);
    }

    /**
     * {@code true} quando alguma violação deve falhar o build (erros sempre; avisos com {@code falharEmAvisos}),
     * nas Regras de arquitetura ou nas de qualidade.
     */
    public boolean falhou() {
        return resultados.stream().anyMatch(configuracao::deveFalhar)
                || qualidade.filter(q -> q.falhou(configuracao.falharEmAvisos())).isPresent();
    }

    /** Violações das Regras de arquitetura e de qualidade somadas. */
    public int totalDeViolacoes() {
        return resultados.stream().mapToInt(resultado -> resultado.violacoes().size()).sum()
                + qualidade.map(VerificacaoDeQualidade::totalDeViolacoes).orElse(0);
    }

    public String json() {
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("ferramenta", "communs-arch");
        raiz.put("versao", versao());
        raiz.put("geradoEm", geradoEm.toString());
        raiz.put("classesAnalisadas", classesAnalisadas);
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("regras", resultados.size());
        resumo.put("aprovadas", resultados.stream().filter(r -> !r.foiIgnorada() && !r.temViolacoes()).count());
        resumo.put("violadas", resultados.stream().filter(ResultadoRegra::temViolacoes).count());
        resumo.put("ignoradas", resultados.stream().filter(ResultadoRegra::foiIgnorada).count());
        resumo.put("violacoes", totalDeViolacoes());
        resumo.put("falhou", falhou());
        raiz.put("resumo", resumo);
        List<Object> regras = new ArrayList<>();
        for (ResultadoRegra resultado : resultados) {
            Map<String, Object> regra = new LinkedHashMap<>();
            regra.put("id", resultado.regra().id());
            regra.put("nome", resultado.regra().nome());
            regra.put("categoria", resultado.regra().categoria().name());
            regra.put("severidade", resultado.regra().severidade().name());
            regra.put("status", status(resultado));
            regra.put("falha", configuracao.deveFalhar(resultado));
            regra.put("motivo", resultado.motivo().orElse(null));
            regra.put("descricao", resultado.avaliacao() == null ? null : resultado.avaliacao().ruleDescription());
            List<Object> violacoes = new ArrayList<>();
            for (ConditionEvent violacao : resultado.violacoes()) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("mensagem", violacao.message());
                item.put("classe", violacao.className().isEmpty() ? null : violacao.className());
                item.put("linha", violacao.lineNumber() > 0 ? violacao.lineNumber() : null);
                violacoes.add(item);
            }
            regra.put("violacoes", violacoes);
            regras.add(regra);
        }
        raiz.put("regras", regras);
        qualidade.ifPresent(verificacao -> raiz.put("qualidade", qualidadeEmJson(verificacao)));
        return Json.escrever(raiz);
    }

    private Map<String, Object> qualidadeEmJson(VerificacaoDeQualidade verificacao) {
        boolean falharEmAvisos = configuracao.falharEmAvisos();
        List<ResultadoQualidade> resultadosQualidade = verificacao.resultados();
        Map<String, Object> raiz = new LinkedHashMap<>();
        raiz.put("projeto", verificacao.projeto().diretorio().toString());
        Map<String, Object> resumo = new LinkedHashMap<>();
        resumo.put("regras", resultadosQualidade.size());
        resumo.put("aprovadas", resultadosQualidade.stream().filter(r -> !r.foiIgnorada() && !r.temViolacoes()).count());
        resumo.put("violadas", resultadosQualidade.stream().filter(ResultadoQualidade::temViolacoes).count());
        resumo.put("ignoradas", resultadosQualidade.stream().filter(ResultadoQualidade::foiIgnorada).count());
        resumo.put("violacoes", verificacao.totalDeViolacoes());
        resumo.put("falhou", verificacao.falhou(falharEmAvisos));
        raiz.put("resumo", resumo);
        List<Object> regras = new ArrayList<>();
        for (ResultadoQualidade resultado : resultadosQualidade) {
            Map<String, Object> regra = new LinkedHashMap<>();
            regra.put("id", resultado.regra().id());
            regra.put("nome", resultado.regra().nome());
            regra.put("severidade", resultado.regra().severidade().name());
            regra.put("status", status(resultado.foiIgnorada(), resultado.temViolacoes()));
            regra.put("falha", resultado.deveFalhar(falharEmAvisos));
            regra.put("motivo", resultado.motivo().orElse(null));
            regra.put("violacoes", resultado.violacoes().stream().map(mensagem -> Map.of("mensagem", mensagem)).toList());
            regras.add(regra);
        }
        raiz.put("regras", regras);
        return raiz;
    }

    public String resumoEmTexto() {
        StringBuilder texto = new StringBuilder();
        if (classesAnalisadas > 0 || qualidade.isEmpty()) {
            texto.append("communs-arch: ").append(classesAnalisadas).append(" classes analisadas, ")
                    .append(resultados.size()).append(" regras").append(System.lineSeparator());
            for (ResultadoRegra resultado : resultados) {
                linha(texto, resultado.regra().id(), resultado.regra().nome(), resultado.foiIgnorada(), resultado.motivo(),
                        configuracao.deveFalhar(resultado),
                        resultado.violacoes().stream().map(ConditionEvent::message).toList());
            }
        }
        qualidade.ifPresent(verificacao -> {
            texto.append("Qualidade: ").append(verificacao.projeto().diretorio()).append(", ")
                    .append(verificacao.resultados().size()).append(" regras").append(System.lineSeparator());
            for (ResultadoQualidade resultado : verificacao.resultados()) {
                linha(texto, resultado.regra().id(), resultado.regra().nome(), resultado.foiIgnorada(), resultado.motivo(),
                        resultado.deveFalhar(configuracao.falharEmAvisos()), resultado.violacoes());
            }
        });
        texto.append(falhou() ? "Resultado: FALHOU" : "Resultado: OK").append(" (").append(totalDeViolacoes()).append(" violações)")
                .append(System.lineSeparator());
        return texto.toString();
    }

    private static void linha(StringBuilder texto, String id, String nome, boolean ignorada, Optional<String> motivo,
                              boolean falha, List<String> violacoes) {
        String marca = ignorada ? "--   " : violacoes.isEmpty() ? "OK   " : falha ? "ERRO " : "AVISO";
        texto.append(marca).append(' ').append(id).append(' ').append(nome);
        if (ignorada) {
            texto.append(" (").append(motivo.orElse("")).append(')');
        } else if (!violacoes.isEmpty()) {
            texto.append(" (").append(violacoes.size()).append(violacoes.size() == 1 ? " violação)" : " violações)");
        }
        texto.append(System.lineSeparator());
        violacoes.forEach(violacao -> texto.append("      ")
                .append(violacao.replace(System.lineSeparator(), System.lineSeparator() + "      "))
                .append(System.lineSeparator()));
    }

    private static String status(ResultadoRegra resultado) {
        return status(resultado.foiIgnorada(), resultado.temViolacoes());
    }

    private static String status(boolean ignorada, boolean temViolacoes) {
        if (ignorada) {
            return "IGNORADA";
        }
        return temViolacoes ? "VIOLADA" : "APROVADA";
    }

    private static String versao() {
        String versao = Relatorio.class.getPackage().getImplementationVersion();
        return versao != null ? versao : "dev";
    }
}

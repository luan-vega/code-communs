package io.github.luanvega.communs.arch.runner;

import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.library.ConfiguracaoRegras;
import io.github.luanvega.communs.arch.library.ResultadoRegra;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Resultado de uma execução do runner, com o JSON que outras ferramentas (CI, chat) consomem
 * e um resumo em texto para o terminal.
 */
public record Relatorio(int classesAnalisadas, List<ResultadoRegra> resultados, ConfiguracaoRegras configuracao, Instant geradoEm) {

    public Relatorio {
        resultados = List.copyOf(resultados);
    }

    /**
     * {@code true} quando alguma violação deve falhar o build (erros sempre; avisos com {@code falharEmAvisos}).
     */
    public boolean falhou() {
        return resultados.stream().anyMatch(configuracao::deveFalhar);
    }

    public int totalDeViolacoes() {
        return resultados.stream().mapToInt(resultado -> resultado.violacoes().size()).sum();
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
        return Json.escrever(raiz);
    }

    public String resumoEmTexto() {
        StringBuilder texto = new StringBuilder();
        texto.append("communs-arch: ").append(classesAnalisadas).append(" classes analisadas, ")
                .append(resultados.size()).append(" regras").append(System.lineSeparator());
        for (ResultadoRegra resultado : resultados) {
            String marca = switch (status(resultado)) {
                case "APROVADA" -> "OK   ";
                case "IGNORADA" -> "--   ";
                default -> configuracao.deveFalhar(resultado) ? "ERRO " : "AVISO";
            };
            texto.append(marca).append(' ').append(resultado.regra().id()).append(' ').append(resultado.regra().nome());
            if (resultado.foiIgnorada()) {
                texto.append(" (").append(resultado.motivo().orElse("")).append(')');
            } else if (resultado.temViolacoes()) {
                texto.append(" (").append(resultado.violacoes().size()).append(resultado.violacoes().size() == 1 ? " violação)" : " violações)");
            }
            texto.append(System.lineSeparator());
            resultado.violacoes().forEach(violacao -> texto.append("      ")
                    .append(violacao.message().replace(System.lineSeparator(), System.lineSeparator() + "      "))
                    .append(System.lineSeparator()));
        }
        texto.append(falhou() ? "Resultado: FALHOU" : "Resultado: OK").append(" (").append(totalDeViolacoes()).append(" violações)")
                .append(System.lineSeparator());
        return texto.toString();
    }

    private static String status(ResultadoRegra resultado) {
        if (resultado.foiIgnorada()) {
            return "IGNORADA";
        }
        return resultado.temViolacoes() ? "VIOLADA" : "APROVADA";
    }

    private static String versao() {
        String versao = Relatorio.class.getPackage().getImplementationVersion();
        return versao != null ? versao : "dev";
    }
}

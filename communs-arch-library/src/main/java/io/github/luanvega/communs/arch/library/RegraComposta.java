package io.github.luanvega.communs.arch.library;

import io.github.luanvega.communs.arch.core.domain.JavaClasses;
import io.github.luanvega.communs.arch.lang.ArchRule;
import io.github.luanvega.communs.arch.lang.ConditionEvent;
import io.github.luanvega.communs.arch.lang.EvaluationResult;
import java.util.ArrayList;
import java.util.List;

/**
 * Várias {@link ArchRule} avaliadas como uma só, com as violações de todas.
 */
final class RegraComposta implements ArchRule {

    private final String descricao;
    private final List<ArchRule> regras;

    RegraComposta(String descricao, List<ArchRule> regras) {
        this.descricao = descricao;
        this.regras = List.copyOf(regras);
    }

    @Override
    public String description() {
        return descricao;
    }

    @Override
    public EvaluationResult evaluate(JavaClasses classes) {
        List<ConditionEvent> violacoes = new ArrayList<>();
        regras.forEach(regra -> violacoes.addAll(regra.evaluate(classes).violations()));
        return new EvaluationResult(descricao, violacoes);
    }

    @Override
    public ArchRule because(String motivo) {
        return new RegraComposta(descricao + ", because " + motivo, regras);
    }

    @Override
    public ArchRule allowEmptyShould(boolean permitir) {
        return new RegraComposta(descricao, regras.stream().map(regra -> regra.allowEmptyShould(permitir)).toList());
    }

    @Override
    public ArchRule as(String novaDescricao) {
        return new RegraComposta(novaDescricao, regras);
    }
}

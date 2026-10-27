package io.github.luanvega.communs.arch.library;

import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Ajustes das Regras para um projeto: padrões de pacote das camadas, pacote raiz, exceção base
 * e regras desligadas. Os padrões de pacote seguem a sintaxe {@code ..camada..}.
 *
 * <pre>{@code
 * ConfiguracaoRegras config = ConfiguracaoRegras.padrao().toBuilder()
 *         .pacoteRaiz("com.exemplo.pedidos")
 *         .excecaoBase("com.exemplo.pedidos.domain.ErroDeNegocio")
 *         .desligar("NOMES-003")
 *         .build();
 * }</pre>
 */
public final class ConfiguracaoRegras {

    private final String pacoteController;
    private final String pacoteService;
    private final String pacoteRepository;
    private final String pacoteDominio;
    private final Set<String> pacotesInfra;
    private final String pacoteRaiz;
    private final String excecaoBase;
    private final Set<String> regrasDesligadas;
    private final boolean falharEmAvisos;

    private ConfiguracaoRegras(Builder builder) {
        this.pacoteController = builder.pacoteController;
        this.pacoteService = builder.pacoteService;
        this.pacoteRepository = builder.pacoteRepository;
        this.pacoteDominio = builder.pacoteDominio;
        this.pacotesInfra = Set.copyOf(builder.pacotesInfra);
        this.pacoteRaiz = builder.pacoteRaiz;
        this.excecaoBase = builder.excecaoBase;
        this.regrasDesligadas = Set.copyOf(builder.regrasDesligadas);
        this.falharEmAvisos = builder.falharEmAvisos;
    }

    public static ConfiguracaoRegras padrao() {
        return builder().build();
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        Builder builder = new Builder();
        builder.pacoteController = pacoteController;
        builder.pacoteService = pacoteService;
        builder.pacoteRepository = pacoteRepository;
        builder.pacoteDominio = pacoteDominio;
        builder.pacotesInfra = new HashSet<>(pacotesInfra);
        builder.pacoteRaiz = pacoteRaiz;
        builder.excecaoBase = excecaoBase;
        builder.regrasDesligadas = new HashSet<>(regrasDesligadas);
        builder.falharEmAvisos = falharEmAvisos;
        return builder;
    }

    public String pacoteController() {
        return pacoteController;
    }

    public String pacoteService() {
        return pacoteService;
    }

    public String pacoteRepository() {
        return pacoteRepository;
    }

    public String pacoteDominio() {
        return pacoteDominio;
    }

    public Set<String> pacotesInfra() {
        return pacotesInfra;
    }

    /**
     * Pacote cujos subpacotes diretos são comparados em {@code CICLOS-001}. Vazio = maior prefixo comum das classes.
     */
    public Optional<String> pacoteRaiz() {
        return Optional.ofNullable(pacoteRaiz);
    }

    /**
     * Exceção da qual as exceções de negócio devem herdar ({@code CODIGO-002}). Vazio = regra ignorada.
     */
    public Optional<String> excecaoBase() {
        return Optional.ofNullable(excecaoBase);
    }

    public boolean estaDesligada(String idRegra) {
        return regrasDesligadas.contains(idRegra);
    }

    public Set<String> regrasDesligadas() {
        return regrasDesligadas;
    }

    /**
     * Se violações de Regras com severidade {@link Severidade#AVISO} também devem falhar o teste ou o build.
     */
    public boolean falharEmAvisos() {
        return falharEmAvisos;
    }

    /**
     * {@code true} quando as violações deste resultado devem falhar o teste ou o build.
     */
    public boolean deveFalhar(ResultadoRegra resultado) {
        return resultado.temViolacoes() && (resultado.regra().severidade() == Severidade.ERRO || falharEmAvisos);
    }

    public static final class Builder {

        private String pacoteController = "..controller..";
        private String pacoteService = "..service..";
        private String pacoteRepository = "..repository..";
        private String pacoteDominio = "..domain..";
        private Set<String> pacotesInfra = new HashSet<>(Set.of("..infra..", "..infrastructure.."));
        private String pacoteRaiz;
        private String excecaoBase;
        private Set<String> regrasDesligadas = new HashSet<>();
        private boolean falharEmAvisos;

        private Builder() {
        }

        public Builder pacoteController(String padrao) {
            this.pacoteController = Objects.requireNonNull(padrao);
            return this;
        }

        public Builder pacoteService(String padrao) {
            this.pacoteService = Objects.requireNonNull(padrao);
            return this;
        }

        public Builder pacoteRepository(String padrao) {
            this.pacoteRepository = Objects.requireNonNull(padrao);
            return this;
        }

        public Builder pacoteDominio(String padrao) {
            this.pacoteDominio = Objects.requireNonNull(padrao);
            return this;
        }

        public Builder pacotesInfra(Set<String> padroes) {
            this.pacotesInfra = new HashSet<>(padroes);
            return this;
        }

        public Builder pacoteRaiz(String pacote) {
            this.pacoteRaiz = pacote;
            return this;
        }

        public Builder excecaoBase(String nomeDaClasse) {
            this.excecaoBase = nomeDaClasse;
            return this;
        }

        public Builder desligar(String... idsRegras) {
            this.regrasDesligadas.addAll(Set.of(idsRegras));
            return this;
        }

        public Builder falharEmAvisos(boolean falhar) {
            this.falharEmAvisos = falhar;
            return this;
        }

        public ConfiguracaoRegras build() {
            return new ConfiguracaoRegras(this);
        }
    }
}

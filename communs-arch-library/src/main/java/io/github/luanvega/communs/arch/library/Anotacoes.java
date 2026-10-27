package io.github.luanvega.communs.arch.library;

import java.util.List;

/**
 * Nomes das anotações de frameworks usadas pelas Regras. Comparadas por nome, para a lib não depender do Spring.
 */
final class Anotacoes {

    static final String REST_CONTROLLER = "org.springframework.web.bind.annotation.RestController";
    static final String CONTROLLER = "org.springframework.stereotype.Controller";
    static final String SERVICE = "org.springframework.stereotype.Service";
    static final String REPOSITORY = "org.springframework.stereotype.Repository";
    static final String SPRING_DATA_REPOSITORY = "org.springframework.data.repository.Repository";
    static final String AUTOWIRED = "org.springframework.beans.factory.annotation.Autowired";
    static final String JAKARTA_INJECT = "jakarta.inject.Inject";
    static final String JAVAX_INJECT = "javax.inject.Inject";
    static final List<String> ENTITY = List.of("jakarta.persistence.Entity", "javax.persistence.Entity");

    private Anotacoes() {
    }
}

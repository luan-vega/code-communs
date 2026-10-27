package io.github.luanvega.communs.arch.junit.exemplos;

import io.github.luanvega.communs.arch.junit.AnalisarClasses;

/** Exemplo executado só pelo EngineTestKit. */
@AnalisarClasses(pacotes = "io.github.luanvega.communs.arch.junit.fixtures.errado", incluirTestes = true, regrasPadrao = true,
        configuracao = "regras-falhar-em-avisos.yaml")
public class FalharEmAvisos {
}

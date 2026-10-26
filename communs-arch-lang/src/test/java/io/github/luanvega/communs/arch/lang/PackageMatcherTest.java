package io.github.luanvega.communs.arch.lang;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.api.Test;

class PackageMatcherTest {

    @ParameterizedTest(name = "''{0}'' casa ''{1}'' = {2}")
    @CsvSource({
            "..service..,         service,                    true",
            "..service..,         com.app.service,            true",
            "..service..,         com.app.service.impl,       true",
            "..service..,         com.app.services,           false",
            "..service..,         com.appservice,             false",
            "com.app..,           com.app,                    true",
            "com.app..,           com.app.pedido.api,         true",
            "com.app..,           com.application,            false",
            "..api,               com.app.api,                true",
            "..api,               com.app.api.v1,             false",
            "com..api,            com.api,                    true",
            "com..api,            com.app.api,                true",
            "com..api,            com.app.pedido.api,         true",
            "com.*.api,           com.app.api,                true",
            "com.*.api,           com.app.pedido.api,         false",
            "..,                  qualquer.coisa,             true",
            "com.app.service,     com.app.service,            true",
            "com.app.service,     com.app.service.impl,       false",
            "..a..b..,            x.a.y.b.z,                  true",
            "..a..b..,            x.b.y.a.z,                  false",
    })
    void casaPadroesNoEstiloArchUnit(String padrao, String pacote, boolean esperado) {
        assertThat(PackageMatcher.of(padrao).matches(pacote)).isEqualTo(esperado);
    }

    @Test
    void recusaTresPontos() {
        assertThatThrownBy(() -> PackageMatcher.of("com...api")).isInstanceOf(IllegalArgumentException.class);
    }
}

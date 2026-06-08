package br.com.inter;


import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.ApplicationContext;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import jakarta.inject.Inject;

@MicronautTest
@Property(name = "micronaut.server.port", value = "-1")
class RemittanceServiceTest {

    @Inject
    EmbeddedApplication<?> application;

    @Test
    void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }

    @Test
    void shouldInstantiateApplication() {
        Assertions.assertNotNull(new Application());
    }

    @Test
    void shouldStartApplicationWithRandomPort() {
        try (ApplicationContext context = Application.start(new String[]{"-Dmicronaut.server.port=-1"})) {
            Assertions.assertTrue(context.isRunning());
        }
    }
}

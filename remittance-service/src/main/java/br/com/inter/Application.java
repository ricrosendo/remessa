package br.com.inter;

import io.micronaut.context.ApplicationContext;
import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                title = "Remittance Service API",
                version = "0.1",
                description = "API para criação de remessas internacionais, conversão BRL/USD, validação de limites diários e integração com cotação PTAX.",
                contact = @Contact(name = "Inter", email = "dev@inter.com.br"),
                license = @License(name = "Apache 2.0")
        )
)
public class Application {

    public static void main(String[] args) {
        start(args);
    }

    static ApplicationContext start(String[] args) {
        return Micronaut.run(Application.class, args);
    }
}
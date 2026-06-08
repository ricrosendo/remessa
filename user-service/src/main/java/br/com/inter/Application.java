package br.com.inter;

import io.micronaut.runtime.Micronaut;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;

@OpenAPIDefinition(
        info = @Info(
                title = "User Service API",
                version = "0.1",
                description = "API para cadastro e manutenção de usuários da plataforma Remessa.",
                contact = @Contact(name = "Inter", email = "dev@inter.com.br"),
                license = @License(name = "Apache 2.0")
        )
)
public class Application {

    public static void main(String[] args) {
        Micronaut.run(Application.class, args);
    }
}
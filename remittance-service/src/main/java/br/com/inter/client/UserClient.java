package br.com.inter.client;

import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UserResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.client.annotation.Client;
import jakarta.validation.Valid;

import java.util.UUID;

@Client("${clients.user-service.url}")
public interface UserClient {

    @Get("/users/{id}")
    UserResponse findById(UUID id);

    @Put("/users/{id}/balance")
    UserResponse updateBalance(UUID id, @Body @Valid UpdateUserBalanceRequest request);
}

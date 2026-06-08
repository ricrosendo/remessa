package br.com.inter.controller;

import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.service.UserService;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Delete;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.validation.Validated;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@Controller("/users")
@Tag(name = "Users", description = "Recursos para cadastro e manutenção de usuários")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Post
    @Operation(summary = "Criar usuário", description = "Cria um usuário pessoa física ou pessoa jurídica.")
    public HttpResponse<UserResponse> create(@Body @Valid CreateUserRequest request) {
        UserResponse response = UserResponse.from(userService.create(request));
        return HttpResponse.created(response).headers(headers -> headers.location(URI.create("/users/" + response.id())));
    }

    @Get
    @Operation(summary = "Listar usuários", description = "Retorna todos os usuários cadastrados.")
    public List<UserResponse> findAll() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @Get("/{id}")
    @Operation(summary = "Buscar usuário por ID", description = "Retorna um usuário pelo identificador UUID.")
    public UserResponse findById(@Parameter(description = "ID do usuário", required = true) UUID id) {
        return UserResponse.from(userService.findById(id));
    }

    @Put("/{id}")
    @Operation(summary = "Atualizar usuário", description = "Atualiza os dados de um usuário existente.")
    public UserResponse update(@Parameter(description = "ID do usuário", required = true) UUID id, @Body @Valid UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    @Put("/{id}/balance")
    @Operation(summary = "Atualizar saldo do usuário", description = "Atualiza os saldos em Real e Dólar de um usuário existente.")
    public UserResponse updateBalance(@Parameter(description = "ID do usuário", required = true) UUID id, @Body @Valid UpdateUserBalanceRequest request) {
        return UserResponse.from(userService.updateBalance(id, request));
    }

    @Delete("/{id}")
    @Operation(summary = "Remover usuário", description = "Remove um usuário existente pelo identificador UUID.")
    public HttpResponse<Void> delete(@Parameter(description = "ID do usuário", required = true) UUID id) {
        userService.delete(id);
        return HttpResponse.noContent();
    }
}

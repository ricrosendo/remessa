package br.com.inter.controller;

import br.com.inter.dto.CreateUserRequest;
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
import jakarta.validation.Valid;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@Validated
@Controller("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Post
    public HttpResponse<UserResponse> create(@Body @Valid CreateUserRequest request) {
        UserResponse response = UserResponse.from(userService.create(request));
        return HttpResponse.created(response).headers(headers -> headers.location(URI.create("/users/" + response.id())));
    }

    @Get
    public List<UserResponse> findAll() {
        return userService.findAll().stream().map(UserResponse::from).toList();
    }

    @Get("/{id}")
    public UserResponse findById(UUID id) {
        return UserResponse.from(userService.findById(id));
    }

    @Put("/{id}")
    public UserResponse update(UUID id, @Body @Valid UpdateUserRequest request) {
        return UserResponse.from(userService.update(id, request));
    }

    @Delete("/{id}")
    public HttpResponse<Void> delete(UUID id) {
        userService.delete(id);
        return HttpResponse.noContent();
    }
}

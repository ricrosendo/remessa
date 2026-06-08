package br.com.inter.controller;

import br.com.inter.dto.CreateUserRequest;
import br.com.inter.dto.ErrorResponse;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UpdateUserRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.UserType;
import br.com.inter.exception.UserAlreadyExistsException;
import br.com.inter.exception.UserNotFoundException;
import br.com.inter.model.User;
import br.com.inter.service.UserService;
import io.micronaut.context.annotation.Property;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.annotation.MockBean;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MicronautTest
@Property(name = "micronaut.server.port", value = "-1")
class UserControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    UserService userService;

    @Test
    void shouldCreateUser() {
        CreateUserRequest request = createRequest();
        User user = userFromCreateRequest(request);
        user.setId(UUID.randomUUID());

        when(userService.create(request)).thenReturn(user);

        HttpResponse<UserResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/users", request),
                UserResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.status());
        assertNotNull(response.body());
        assertEquals(user.getId(), response.body().id());
        assertEquals(request.fullName(), response.body().fullName());
        assertTrue(response.getHeaders().contains("Location"));
        verify(userService).create(request);
    }

    @Test
    void shouldFindAllUsers() {
        User user = userFromCreateRequest(createRequest());
        user.setId(UUID.randomUUID());

        when(userService.findAll()).thenReturn(List.of(user));

        HttpResponse<UserResponse[]> response = httpClient.toBlocking().exchange(
                HttpRequest.GET("/api/users"),
                UserResponse[].class
        );

        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.body());
        assertEquals(1, response.body().length);
        assertEquals(user.getId(), response.body()[0].id());
        verify(userService).findAll();
    }

    @Test
    void shouldFindUserById() {
        UUID id = UUID.randomUUID();
        User user = userFromCreateRequest(createRequest());
        user.setId(id);

        when(userService.findById(id)).thenReturn(user);

        HttpResponse<UserResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.GET("/api/users/" + id),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.body());
        assertEquals(id, response.body().id());
        verify(userService).findById(id);
    }

    @Test
    void shouldUpdateUser() {
        UUID id = UUID.randomUUID();
        UpdateUserRequest request = updateRequest();
        User user = userFromUpdateRequest(request);
        user.setId(id);

        when(userService.update(id, request)).thenReturn(user);

        HttpResponse<UserResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.PUT("/api/users/" + id, request),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.body());
        assertEquals(id, response.body().id());
        assertEquals(request.fullName(), response.body().fullName());
        verify(userService).update(id, request);
    }

    @Test
    void shouldUpdateUserBalance() {
        UUID id = UUID.randomUUID();
        UpdateUserBalanceRequest request = new UpdateUserBalanceRequest(BigDecimal.valueOf(250), BigDecimal.valueOf(50));
        User user = userFromCreateRequest(createRequest());
        user.setId(id);
        user.setBrlBalance(request.brlBalance());
        user.setUsdBalance(request.usdBalance());

        when(userService.updateBalance(id, request)).thenReturn(user);

        HttpResponse<UserResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.PUT("/api/users/" + id + "/balance", request),
                UserResponse.class
        );

        assertEquals(HttpStatus.OK, response.status());
        assertNotNull(response.body());
        assertEquals(request.brlBalance(), response.body().brlBalance());
        assertEquals(request.usdBalance(), response.body().usdBalance());
        verify(userService).updateBalance(id, request);
    }

    @Test
    void shouldDeleteUser() {
        UUID id = UUID.randomUUID();

        HttpResponse<Void> response = httpClient.toBlocking().exchange(
                HttpRequest.DELETE("/api/users/" + id),
                Void.class
        );

        assertEquals(HttpStatus.NO_CONTENT, response.status());
        verify(userService).delete(id);
    }

    @Test
    void shouldReturnNotFoundWhenUserDoesNotExist() {
        UUID id = UUID.randomUUID();

        when(userService.findById(id)).thenThrow(new UserNotFoundException(id));

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().exchange(
                HttpRequest.GET("/api/users/" + id),
                ErrorResponse.class
        ));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatus());
        ErrorResponse errorResponse = exception.getResponse().getBody(ErrorResponse.class).orElseThrow();
        assertEquals("User not found with id: " + id, errorResponse.message());
        verify(userService).findById(id);
    }

    @Test
    void shouldReturnBadRequestWhenUserAlreadyExists() {
        CreateUserRequest request = createRequest();

        when(userService.create(request)).thenThrow(new UserAlreadyExistsException("email"));

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/users", request),
                ErrorResponse.class
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        ErrorResponse errorResponse = exception.getResponse().getBody(ErrorResponse.class).orElseThrow();
        assertEquals("User already exists with email", errorResponse.message());
        verify(userService).create(request);
    }

    @MockBean(UserService.class)
    UserService userService() {
        return mock(UserService.class);
    }

    private CreateUserRequest createRequest() {
        return new CreateUserRequest(
                "John Doe",
                "john.doe@email.com",
                "password",
                UserType.INDIVIDUAL,
                "12345678901",
                null,
                BigDecimal.TEN,
                BigDecimal.ONE
        );
    }

    private UpdateUserRequest updateRequest() {
        return new UpdateUserRequest(
                "Jane Doe",
                "jane.doe@email.com",
                "new-password",
                UserType.INDIVIDUAL,
                "98765432100",
                null,
                BigDecimal.valueOf(20),
                BigDecimal.valueOf(2)
        );
    }

    private User userFromCreateRequest(CreateUserRequest request) {
        return User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(request.password())
                .type(request.type())
                .cpf(request.cpf())
                .cnpj(request.cnpj())
                .brlBalance(request.brlBalance())
                .usdBalance(request.usdBalance())
                .build();
    }

    private User userFromUpdateRequest(UpdateUserRequest request) {
        return User.builder()
                .fullName(request.fullName())
                .email(request.email())
                .password(request.password())
                .type(request.type())
                .cpf(request.cpf())
                .cnpj(request.cnpj())
                .brlBalance(request.brlBalance())
                .usdBalance(request.usdBalance())
                .build();
    }
}

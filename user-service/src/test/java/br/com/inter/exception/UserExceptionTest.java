package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class UserExceptionTest {

    @Test
    void shouldExposeUserNotFoundExceptionMessage() {
        UUID id = UUID.randomUUID();
        UserNotFoundException exception = new UserNotFoundException(id);

        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(UserException.class, exception);
        assertEquals("User not found with id: " + id, exception.getMessage());
        assertEquals(exception.getMessage(), exception.message());
    }

    @Test
    void shouldExposeUserAlreadyExistsExceptionMessage() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("email");

        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(UserException.class, exception);
        assertEquals("User already exists with email", exception.getMessage());
        assertEquals(exception.getMessage(), exception.message());
    }

    @Test
    void shouldExposeInvalidUserDocumentExceptionMessage() {
        InvalidUserDocumentException exception = new InvalidUserDocumentException("CPF is required for individual users");

        assertInstanceOf(RuntimeException.class, exception);
        assertInstanceOf(UserException.class, exception);
        assertEquals("CPF is required for individual users", exception.getMessage());
        assertEquals(exception.getMessage(), exception.message());
    }

    @Test
    void shouldHandleUserNotFoundException() {
        UUID id = UUID.randomUUID();
        UserNotFoundException exception = new UserNotFoundException(id);
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        HttpResponse<ErrorResponse> response = handler.handle(null, exception);

        assertEquals(HttpStatus.NOT_FOUND, response.status());
        assertEquals(exception.message(), response.body().message());
    }

    @Test
    void shouldHandleUserAlreadyExistsException() {
        UserAlreadyExistsException exception = new UserAlreadyExistsException("email");
        UserAlreadyExistsExceptionHandler handler = new UserAlreadyExistsExceptionHandler();

        HttpResponse<ErrorResponse> response = handler.handle(null, exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.status());
        assertEquals(exception.message(), response.body().message());
    }

    @Test
    void shouldHandleInvalidUserDocumentException() {
        InvalidUserDocumentException exception = new InvalidUserDocumentException("CNPJ is required for company users");
        InvalidUserDocumentExceptionHandler handler = new InvalidUserDocumentExceptionHandler();

        HttpResponse<ErrorResponse> response = handler.handle(null, exception);

        assertEquals(HttpStatus.BAD_REQUEST, response.status());
        assertEquals(exception.message(), response.body().message());
    }
}

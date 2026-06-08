package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import jakarta.inject.Singleton;

@Produces
@Singleton
public class GlobalExceptionHandler {

    @Error(global = true, exception = UserNotFoundException.class)
    public HttpResponse<ErrorResponse> handleNotFound(UserNotFoundException exception) {
        return HttpResponse.notFound(new ErrorResponse(exception.message()));
    }

    @Error(global = true, exception = UserAlreadyExistsException.class)
    public HttpResponse<ErrorResponse> handleAlreadyExists(UserAlreadyExistsException exception) {
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }

    @Error(global = true, exception = InvalidUserDocumentException.class)
    public HttpResponse<ErrorResponse> handleInvalidDocument(InvalidUserDocumentException exception) {
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }
}

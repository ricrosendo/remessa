package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Produces
@Singleton
public class GlobalExceptionHandler implements ExceptionHandler<UserNotFoundException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UserNotFoundException exception) {
        return HttpResponse.notFound(new ErrorResponse(exception.message()));
    }
}

@Produces
@Singleton
class UserAlreadyExistsExceptionHandler implements ExceptionHandler<UserAlreadyExistsException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UserAlreadyExistsException exception) {
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }
}

@Produces
@Singleton
class InvalidUserDocumentExceptionHandler implements ExceptionHandler<InvalidUserDocumentException, HttpResponse<ErrorResponse>> {

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, InvalidUserDocumentException exception) {
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }
}
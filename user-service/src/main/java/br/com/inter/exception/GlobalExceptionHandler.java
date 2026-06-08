package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@Produces
@Singleton
public class GlobalExceptionHandler implements ExceptionHandler<UserNotFoundException, HttpResponse<ErrorResponse>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UserNotFoundException exception) {
        LOGGER.warn("Handling user not found exception. method={}, path={}, message={}", requestMethod(request), requestPath(request), exception.message());
        return HttpResponse.notFound(new ErrorResponse(exception.message()));
    }

    static Object requestMethod(HttpRequest request) {
        return Objects.nonNull(request) ? request.getMethod() : "unknown";
    }

    static String requestPath(HttpRequest request) {
        return Objects.nonNull(request) ? request.getPath() : "unknown";
    }
}

@Produces
@Singleton
class UserAlreadyExistsExceptionHandler implements ExceptionHandler<UserAlreadyExistsException, HttpResponse<ErrorResponse>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UserAlreadyExistsExceptionHandler.class);

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, UserAlreadyExistsException exception) {
        LOGGER.warn("Handling user already exists exception. method={}, path={}, message={}", GlobalExceptionHandler.requestMethod(request), GlobalExceptionHandler.requestPath(request), exception.message());
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }
}

@Produces
@Singleton
class InvalidUserDocumentExceptionHandler implements ExceptionHandler<InvalidUserDocumentException, HttpResponse<ErrorResponse>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvalidUserDocumentExceptionHandler.class);

    @Override
    public HttpResponse<ErrorResponse> handle(HttpRequest request, InvalidUserDocumentException exception) {
        LOGGER.warn("Handling invalid user document exception. method={}, path={}, message={}", GlobalExceptionHandler.requestMethod(request), GlobalExceptionHandler.requestPath(request), exception.message());
        return HttpResponse.badRequest(new ErrorResponse(exception.message()));
    }
}
package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;

@Singleton
@Produces
public class GlobalExceptionHandler implements ExceptionHandler<RemittanceException, HttpResponse<ErrorResponse>> {

    @Override
    @Error(global = true)
    public HttpResponse<ErrorResponse> handle(io.micronaut.http.HttpRequest request, RemittanceException exception) {
        return HttpResponse.badRequest(new ErrorResponse(exception.getMessage()));
    }
}

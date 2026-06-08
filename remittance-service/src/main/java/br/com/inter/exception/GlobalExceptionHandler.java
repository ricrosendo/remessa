package br.com.inter.exception;

import br.com.inter.dto.ErrorResponse;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Error;
import io.micronaut.http.annotation.Produces;
import io.micronaut.http.server.exceptions.ExceptionHandler;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Produces
public class GlobalExceptionHandler implements ExceptionHandler<RemittanceException, HttpResponse<ErrorResponse>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @Override
    @Error(global = true)
    public HttpResponse<ErrorResponse> handle(HttpRequest request, RemittanceException exception) {
        LOGGER.warn("Handling remittance exception. method={}, path={}, message={}", request.getMethod(), request.getPath(), exception.getMessage());
        return HttpResponse.badRequest(new ErrorResponse(exception.getMessage()));
    }
}
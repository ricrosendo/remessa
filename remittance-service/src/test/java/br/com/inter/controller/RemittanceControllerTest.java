package br.com.inter.controller;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.ErrorResponse;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.dto.UserResponse;
import br.com.inter.exception.RemittanceException;
import br.com.inter.service.RemittanceService;
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
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@MicronautTest
@Property(name = "micronaut.server.port", value = "-1")
class RemittanceControllerTest {

    @Inject
    @Client("/")
    HttpClient httpClient;

    @Inject
    RemittanceService remittanceService;

    @Test
    void shouldCreateRemittance() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = request(senderId, receiverId);
        RemittanceResponse serviceResponse = response(senderId, receiverId);

        when(remittanceService.create(request)).thenReturn(serviceResponse);

        HttpResponse<RemittanceResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/remittances", request),
                RemittanceResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.status());
        assertNotNull(response.body());
        assertEquals(senderId, response.body().senderUserId());
        assertEquals(receiverId, response.body().receiverUserId());
        assertEquals(BigDecimal.valueOf(500), response.body().brlAmount());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.body().usdAmount());
        verify(remittanceService).create(request);
    }

    @Test
    void shouldReturnBadRequestWhenRemittanceServiceThrowsException() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = request(senderId, receiverId);

        when(remittanceService.create(request)).thenThrow(new RemittanceException("Insufficient BRL balance for remittance"));

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/remittances", request),
                ErrorResponse.class
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        ErrorResponse errorResponse = exception.getResponse().getBody(ErrorResponse.class).orElseThrow();
        assertEquals("Insufficient BRL balance for remittance", errorResponse.message());
        verify(remittanceService).create(request);
    }

    @MockBean(RemittanceService.class)
    RemittanceService remittanceService() {
        return mock(RemittanceService.class);
    }

    private CreateRemittanceRequest request(UUID senderId, UUID receiverId) {
        return new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));
    }

    private RemittanceResponse response(UUID senderId, UUID receiverId) {
        return new RemittanceResponse(
                senderId,
                receiverId,
                BigDecimal.valueOf(500),
                BigDecimal.valueOf(100).setScale(2),
                BigDecimal.valueOf(5),
                LocalDate.of(2025, 1, 30),
                user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO),
                user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))
        );
    }

    private UserResponse user(UUID id, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "User", "user@email.com", "INDIVIDUAL", "12345678901", null, brlBalance, usdBalance);
    }
}

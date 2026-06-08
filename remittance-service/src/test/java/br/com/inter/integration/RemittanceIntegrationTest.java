package br.com.inter.integration;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.ErrorResponse;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UserResponse;
import io.micronaut.context.annotation.Property;
import io.micronaut.context.annotation.Requires;
import io.micronaut.core.annotation.Nullable;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Put;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
@Property(name = "micronaut.server.port", value = "18082")
@Property(name = "clients.user-service.url", value = "http://localhost:18082/api/integration/users")
@Property(name = "clients.ptax.url", value = "http://localhost:18082/api/integration/ptax")
@Property(name = "spec.name", value = "RemittanceIntegrationTest")
class RemittanceIntegrationTest {

    private static final UUID SENDER_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID RECEIVER_ID = UUID.fromString("00000000-0000-0000-0000-000000000002");

    @Inject
    @Client("/")
    HttpClient httpClient;

    @BeforeEach
    void setUp() {
        FakeUserController.reset();
        FakePtaxController.reset();
    }

    @Test
    void shouldCreateRemittanceUsingHttpClientsAndExternalServiceStubs() {
        CreateRemittanceRequest request = new CreateRemittanceRequest(SENDER_ID, RECEIVER_ID, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        HttpResponse<RemittanceResponse> response = httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/remittances", request),
                RemittanceResponse.class
        );

        assertEquals(HttpStatus.CREATED, response.status());
        assertNotNull(response.body());
        assertEquals(SENDER_ID, response.body().senderUserId());
        assertEquals(RECEIVER_ID, response.body().receiverUserId());
        assertEquals(BigDecimal.valueOf(500), response.body().brlAmount());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.body().usdAmount());
        assertEquals(BigDecimal.valueOf(5), response.body().exchangeRate());
        assertEquals(BigDecimal.valueOf(500), response.body().sender().brlBalance());
        assertEquals(BigDecimal.valueOf(110).setScale(2), response.body().receiver().usdBalance());
        assertEquals("'01-30-2025'", FakePtaxController.lastQuotationDate);
        assertEquals(100, FakePtaxController.lastTop);
        assertEquals("json", FakePtaxController.lastFormat);
    }

    @Test
    void shouldReturnBadRequestWhenSenderHasInsufficientBalance() {
        FakeUserController.users.put(SENDER_ID, user(SENDER_ID, BigDecimal.valueOf(100), BigDecimal.ZERO));
        CreateRemittanceRequest request = new CreateRemittanceRequest(SENDER_ID, RECEIVER_ID, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        HttpClientResponseException exception = assertThrows(HttpClientResponseException.class, () -> httpClient.toBlocking().exchange(
                HttpRequest.POST("/api/remittances", request),
                ErrorResponse.class
        ));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatus());
        ErrorResponse errorResponse = exception.getResponse().getBody(ErrorResponse.class).orElseThrow();
        assertEquals("Insufficient BRL balance for remittance", errorResponse.message());
        assertEquals(BigDecimal.valueOf(100), FakeUserController.users.get(SENDER_ID).brlBalance());
        assertEquals(BigDecimal.TEN, FakeUserController.users.get(RECEIVER_ID).usdBalance());
    }

    @Requires(property = "spec.name", value = "RemittanceIntegrationTest")
    @Controller("/integration/users/users")
    static class FakeUserController {

        static final Map<UUID, UserResponse> users = new ConcurrentHashMap<>();

        static void reset() {
            users.clear();
            users.put(SENDER_ID, user(SENDER_ID, BigDecimal.valueOf(1000), BigDecimal.ZERO));
            users.put(RECEIVER_ID, user(RECEIVER_ID, BigDecimal.ZERO, BigDecimal.TEN));
        }

        @Get("/{id}")
        UserResponse findById(UUID id) {
            return users.get(id);
        }

        @Put("/{id}/balance")
        UserResponse updateBalance(UUID id, @Body UpdateUserBalanceRequest request) {
            UserResponse current = users.get(id);
            UserResponse updated = new UserResponse(
                    current.id(),
                    current.fullName(),
                    current.email(),
                    current.type(),
                    current.cpf(),
                    current.cnpj(),
                    request.brlBalance(),
                    request.usdBalance()
            );
            users.put(id, updated);
            return updated;
        }
    }

    @Requires(property = "spec.name", value = "RemittanceIntegrationTest")
    @Controller("/integration/ptax")
    static class FakePtaxController {

        static String lastQuotationDate;
        static Integer lastTop;
        static String lastFormat;

        static void reset() {
            lastQuotationDate = null;
            lastTop = null;
            lastFormat = null;
        }

        @Get("/CotacaoDolarDia(dataCotacao=@dataCotacao)")
        PtaxQuotationResponse findDollarQuotation(
                @Nullable @QueryValue("@dataCotacao") String dataCotacao,
                @Nullable @QueryValue("$top") Integer top,
                @Nullable @QueryValue("$format") String format
        ) {
            lastQuotationDate = dataCotacao;
            lastTop = top;
            lastFormat = format;
            return new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5))));
        }
    }

    private static UserResponse user(UUID id, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "User", "user@email.com", "INDIVIDUAL", "12345678901", null, brlBalance, usdBalance);
    }
}

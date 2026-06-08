package br.com.inter.service.impl;

import br.com.inter.client.PtaxClient;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.exception.RemittanceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class PtaxExchangeRateServiceTest {

    @Mock
    private PtaxClient ptaxClient;

    private PtaxExchangeRateService exchangeRateService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        exchangeRateService = new PtaxExchangeRateService(ptaxClient);
    }

    @Test
    void shouldReturnExchangeRateFromPtax() {
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);

        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));

        BigDecimal exchangeRate = exchangeRateService.findExchangeRate(quotationDate);

        assertEquals(BigDecimal.valueOf(5), exchangeRate);
    }

    @Test
    void shouldReturnFirstNonNullExchangeRateFromPtax() {
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);

        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(null), new PtaxQuotation(BigDecimal.valueOf(4)))));

        BigDecimal exchangeRate = exchangeRateService.findExchangeRate(quotationDate);

        assertEquals(BigDecimal.valueOf(4), exchangeRate);
    }

    @Test
    void shouldUseCachedExchangeRateWhenPtaxDoesNotReturnQuotation() {
        LocalDate friday = LocalDate.of(2025, 1, 31);
        LocalDate saturday = LocalDate.of(2025, 2, 1);

        when(ptaxClient.findDollarQuotation("'01-31-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(ptaxClient.findDollarQuotation("'02-01-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of()));

        BigDecimal firstExchangeRate = exchangeRateService.findExchangeRate(friday);
        BigDecimal cachedExchangeRate = exchangeRateService.findExchangeRate(saturday);

        assertEquals(BigDecimal.valueOf(5), firstExchangeRate);
        assertEquals(BigDecimal.valueOf(5), cachedExchangeRate);
    }

    @Test
    void shouldThrowExceptionWhenPtaxDoesNotReturnQuotationAndCacheIsEmpty() {
        LocalDate quotationDate = LocalDate.of(2025, 2, 1);

        when(ptaxClient.findDollarQuotation("'02-01-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of()));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> exchangeRateService.findExchangeRate(quotationDate));

        assertEquals("Dollar quotation not found for date: 2025-02-01", exception.getMessage());
    }
}

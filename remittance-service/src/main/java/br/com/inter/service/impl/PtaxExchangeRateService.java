package br.com.inter.service.impl;

import br.com.inter.client.PtaxClient;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.exception.RemittanceException;
import br.com.inter.service.ExchangeRateService;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class PtaxExchangeRateService implements ExchangeRateService {

    private static final DateTimeFormatter PTAX_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final PtaxClient ptaxClient;
    private final AtomicReference<BigDecimal> lastExchangeRate = new AtomicReference<>();

    public PtaxExchangeRateService(PtaxClient ptaxClient) {
        this.ptaxClient = ptaxClient;
    }

    @Override
    public BigDecimal findExchangeRate(LocalDate quotationDate) {
        String formattedQuotationDate = "'" + quotationDate.format(PTAX_DATE_FORMATTER) + "'";
        PtaxQuotationResponse response = ptaxClient.findDollarQuotation(formattedQuotationDate, 100, "json");

        return response.value().stream()
                .map(PtaxQuotation::cotacaoCompra)
                .filter(Objects::nonNull)
                .findFirst()
                .map(exchangeRate -> {
                    lastExchangeRate.set(exchangeRate);
                    return exchangeRate;
                })
                .orElseGet(() -> cachedExchangeRate(quotationDate));
    }

    private BigDecimal cachedExchangeRate(LocalDate quotationDate) {
        BigDecimal exchangeRate = lastExchangeRate.get();

        if (exchangeRate == null) {
            throw new RemittanceException("Dollar quotation not found for date: " + quotationDate);
        }

        return exchangeRate;
    }
}

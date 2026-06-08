package br.com.inter.service.impl;

import br.com.inter.client.PtaxClient;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.exception.RemittanceException;
import br.com.inter.service.ExchangeRateService;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class PtaxExchangeRateService implements ExchangeRateService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PtaxExchangeRateService.class);
    private static final DateTimeFormatter PTAX_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final PtaxClient ptaxClient;
    private final AtomicReference<BigDecimal> lastExchangeRate = new AtomicReference<>();

    public PtaxExchangeRateService(PtaxClient ptaxClient) {
        this.ptaxClient = ptaxClient;
    }

    @Override
    public BigDecimal findExchangeRate(LocalDate quotationDate) {
        String formattedQuotationDate = "'" + quotationDate.format(PTAX_DATE_FORMATTER) + "'";
        LOGGER.info("Fetching PTAX exchange rate. quotationDate={}, formattedQuotationDate={}", quotationDate, formattedQuotationDate);
        PtaxQuotationResponse response = ptaxClient.findDollarQuotation(formattedQuotationDate, 100, "json");

        return response.value().stream()
                .map(PtaxQuotation::cotacaoCompra)
                .filter(Objects::nonNull)
                .findFirst()
                .map(exchangeRate -> {
                    lastExchangeRate.set(exchangeRate);
                    LOGGER.info("PTAX exchange rate found and cached. quotationDate={}", quotationDate);
                    return exchangeRate;
                })
                .orElseGet(() -> cachedExchangeRate(quotationDate));
    }

    private BigDecimal cachedExchangeRate(LocalDate quotationDate) {
        BigDecimal exchangeRate = lastExchangeRate.get();

        if (exchangeRate == null) {
            LOGGER.warn("PTAX exchange rate not found and cache is empty. quotationDate={}", quotationDate);
            throw new RemittanceException("Dollar quotation not found for date: " + quotationDate);
        }

        LOGGER.warn("Using cached PTAX exchange rate. quotationDate={}", quotationDate);
        return exchangeRate;
    }
}
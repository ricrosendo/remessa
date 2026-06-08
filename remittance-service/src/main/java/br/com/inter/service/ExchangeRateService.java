package br.com.inter.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ExchangeRateService {

    BigDecimal findExchangeRate(LocalDate quotationDate);
}

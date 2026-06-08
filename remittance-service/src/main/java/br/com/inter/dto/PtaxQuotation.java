package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;

@Serdeable
public record PtaxQuotation(
        BigDecimal cotacaoCompra
) {
}

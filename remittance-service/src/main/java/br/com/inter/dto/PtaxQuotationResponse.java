package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.util.List;

@Serdeable
public record PtaxQuotationResponse(
        List<PtaxQuotation> value
) {
}

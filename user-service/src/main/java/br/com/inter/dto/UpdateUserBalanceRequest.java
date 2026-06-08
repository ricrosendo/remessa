package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Serdeable
public record UpdateUserBalanceRequest(
        @NotNull BigDecimal brlBalance,
        @NotNull BigDecimal usdBalance
) {
}

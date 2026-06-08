package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Serdeable
public record CreateRemittanceRequest(
        @NotNull UUID senderUserId,
        @NotNull UUID receiverUserId,
        @NotNull @DecimalMin("0.01") BigDecimal brlAmount,
        @NotNull LocalDate quotationDate
) {
}

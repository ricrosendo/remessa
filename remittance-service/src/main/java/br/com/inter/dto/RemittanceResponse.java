package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Serdeable
public record RemittanceResponse(
        UUID senderUserId,
        UUID receiverUserId,
        BigDecimal brlAmount,
        BigDecimal usdAmount,
        BigDecimal exchangeRate,
        LocalDate quotationDate,
        UserResponse sender,
        UserResponse receiver
) {
}

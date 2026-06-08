package br.com.inter.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

@Serdeable
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        String type,
        String cpf,
        String cnpj,
        BigDecimal brlBalance,
        BigDecimal usdBalance
) {
}

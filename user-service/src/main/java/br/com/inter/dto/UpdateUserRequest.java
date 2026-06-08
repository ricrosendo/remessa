package br.com.inter.dto;

import br.com.inter.enums.UserType;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Serdeable
public record UpdateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull UserType type,
        String cpf,
        String cnpj,
        @NotNull @DecimalMin("0.00") BigDecimal brlBalance,
        @NotNull @DecimalMin("0.00") BigDecimal usdBalance
) {
}

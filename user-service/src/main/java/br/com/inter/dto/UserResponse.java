package br.com.inter.dto;

import br.com.inter.model.User;
import br.com.inter.enums.UserType;
import io.micronaut.serde.annotation.Serdeable;

import java.math.BigDecimal;
import java.util.UUID;

@Serdeable
public record UserResponse(
        UUID id,
        String fullName,
        String email,
        UserType type,
        String cpf,
        String cnpj,
        BigDecimal brlBalance,
        BigDecimal usdBalance
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getType(),
                user.getCpf(),
                user.getCnpj(),
                user.getBrlBalance(),
                user.getUsdBalance()
        );
    }
}

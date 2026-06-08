package br.com.inter.validator;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.RemittanceStatus;
import br.com.inter.exception.RemittanceException;
import br.com.inter.repository.RemittanceRepository;
import jakarta.inject.Singleton;

import java.math.BigDecimal;

@Singleton
public class DailyRemittanceLimitValidator {

    private static final BigDecimal INDIVIDUAL_DAILY_LIMIT = BigDecimal.valueOf(10000);
    private static final BigDecimal COMPANY_DAILY_LIMIT = BigDecimal.valueOf(50000);

    private final RemittanceRepository remittanceRepository;

    public DailyRemittanceLimitValidator(RemittanceRepository remittanceRepository) {
        this.remittanceRepository = remittanceRepository;
    }

    public void validate(UserResponse sender, CreateRemittanceRequest request) {
        BigDecimal dailyLimit = dailyLimitFor(sender);
        BigDecimal dailyTotal = remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(
                sender.id(),
                request.quotationDate(),
                RemittanceStatus.COMPLETED
        );

        if (dailyTotal.add(request.brlAmount()).compareTo(dailyLimit) > 0) {
            throw new RemittanceException("Daily remittance limit exceeded for user type " + sender.type());
        }
    }

    private BigDecimal dailyLimitFor(UserResponse sender) {
        return switch (sender.type()) {
            case "INDIVIDUAL" -> INDIVIDUAL_DAILY_LIMIT;
            case "COMPANY" -> COMPANY_DAILY_LIMIT;
            default -> throw new RemittanceException("Unsupported user type for remittance: " + sender.type());
        };
    }
}

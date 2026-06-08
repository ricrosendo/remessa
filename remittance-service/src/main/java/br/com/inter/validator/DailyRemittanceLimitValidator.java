package br.com.inter.validator;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.RemittanceStatus;
import br.com.inter.exception.RemittanceException;
import br.com.inter.repository.RemittanceRepository;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;

@Singleton
public class DailyRemittanceLimitValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(DailyRemittanceLimitValidator.class);
    private static final BigDecimal INDIVIDUAL_DAILY_LIMIT = BigDecimal.valueOf(10000);
    private static final BigDecimal COMPANY_DAILY_LIMIT = BigDecimal.valueOf(50000);

    private final RemittanceRepository remittanceRepository;

    public DailyRemittanceLimitValidator(RemittanceRepository remittanceRepository) {
        this.remittanceRepository = remittanceRepository;
    }

    public void validate(UserResponse sender, CreateRemittanceRequest request) {
        LOGGER.debug("Validating daily remittance limit. senderUserId={}, userType={}, quotationDate={}", sender.id(), sender.type(), request.quotationDate());
        BigDecimal dailyLimit = dailyLimitFor(sender);
        BigDecimal dailyTotal = remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(
                sender.id(),
                request.quotationDate(),
                RemittanceStatus.COMPLETED
        );

        BigDecimal projectedDailyTotal = dailyTotal.add(request.brlAmount());

        if (projectedDailyTotal.compareTo(dailyLimit) > 0) {
            LOGGER.warn("Daily remittance limit exceeded. senderUserId={}, userType={}, quotationDate={}", sender.id(), sender.type(), request.quotationDate());
            throw new RemittanceException("Daily remittance limit exceeded for user type " + sender.type());
        }

        LOGGER.debug("Daily remittance limit approved. senderUserId={}, userType={}, quotationDate={}", sender.id(), sender.type(), request.quotationDate());
    }

    private BigDecimal dailyLimitFor(UserResponse sender) {
        return switch (sender.type()) {
            case "INDIVIDUAL" -> INDIVIDUAL_DAILY_LIMIT;
            case "COMPANY" -> COMPANY_DAILY_LIMIT;
            default -> {
                LOGGER.warn("Unsupported user type for daily remittance limit. senderUserId={}, userType={}", sender.id(), sender.type());
                throw new RemittanceException("Unsupported user type for remittance: " + sender.type());
            }
        };
    }
}
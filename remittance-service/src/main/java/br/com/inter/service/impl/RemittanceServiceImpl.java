package br.com.inter.service.impl;

import br.com.inter.client.PtaxClient;
import br.com.inter.client.UserClient;
import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.RemittanceStatus;
import br.com.inter.exception.RemittanceException;
import br.com.inter.model.Remittance;
import br.com.inter.repository.RemittanceRepository;
import br.com.inter.service.RemittanceService;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class RemittanceServiceImpl implements RemittanceService {

    private static final DateTimeFormatter PTAX_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");
    private static final BigDecimal INDIVIDUAL_DAILY_LIMIT = BigDecimal.valueOf(10000);
    private static final BigDecimal COMPANY_DAILY_LIMIT = BigDecimal.valueOf(50000);

    private final UserClient userClient;
    private final PtaxClient ptaxClient;
    private final RemittanceRepository remittanceRepository;
    private final AtomicReference<BigDecimal> lastExchangeRate = new AtomicReference<>();

    public RemittanceServiceImpl(UserClient userClient, PtaxClient ptaxClient, RemittanceRepository remittanceRepository) {
        this.userClient = userClient;
        this.ptaxClient = ptaxClient;
        this.remittanceRepository = remittanceRepository;
    }

    @Override
    public RemittanceResponse create(CreateRemittanceRequest request) {
        if (request.senderUserId().equals(request.receiverUserId())) {
            throw new RemittanceException("Sender and receiver users must be different");
        }

        UserResponse sender = userClient.findById(request.senderUserId());
        UserResponse receiver = userClient.findById(request.receiverUserId());
        validateDailyLimit(sender, request);
        BigDecimal exchangeRate = findExchangeRate(request);

        if (sender.brlBalance().compareTo(request.brlAmount()) < 0) {
            throw new RemittanceException("Insufficient BRL balance for remittance");
        }

        BigDecimal usdAmount = request.brlAmount().divide(exchangeRate, 2, RoundingMode.HALF_UP);
        Remittance remittance = createPendingRemittance(request, exchangeRate, usdAmount);

        UserResponse updatedSender = null;

        try {
            updatedSender = debitSender(sender, request);
            updateStatus(remittance, RemittanceStatus.SENDER_DEBITED, null);

            UserResponse updatedReceiver = creditReceiver(receiver, usdAmount);
            updateStatus(remittance, RemittanceStatus.COMPLETED, null);
            remittance.setCompletedAt(LocalDateTime.now());
            remittanceRepository.update(remittance);

            return new RemittanceResponse(
                    sender.id(),
                    receiver.id(),
                    request.brlAmount(),
                    usdAmount,
                    exchangeRate,
                    request.quotationDate(),
                    updatedSender,
                    updatedReceiver
            );
        } catch (Exception exception) {
            compensateSenderIfNeeded(sender, updatedSender);
            updateStatus(remittance, updatedSender == null ? RemittanceStatus.FAILED : RemittanceStatus.COMPENSATED, exception.getMessage());
            throw new RemittanceException("Remittance transaction failed: " + exception.getMessage());
        }
    }

    private void validateDailyLimit(UserResponse sender, CreateRemittanceRequest request) {
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

    private Remittance createPendingRemittance(CreateRemittanceRequest request, BigDecimal exchangeRate, BigDecimal usdAmount) {
        Remittance remittance = new Remittance();
        remittance.setSenderUserId(request.senderUserId());
        remittance.setReceiverUserId(request.receiverUserId());
        remittance.setBrlAmount(request.brlAmount());
        remittance.setUsdAmount(usdAmount);
        remittance.setExchangeRate(exchangeRate);
        remittance.setQuotationDate(request.quotationDate());
        remittance.setStatus(RemittanceStatus.PENDING);
        remittance.setCreatedAt(LocalDateTime.now());
        return remittanceRepository.save(remittance);
    }

    private UserResponse debitSender(UserResponse sender, CreateRemittanceRequest request) {
        return userClient.updateBalance(
                sender.id(),
                new UpdateUserBalanceRequest(sender.brlBalance().subtract(request.brlAmount()), sender.usdBalance())
        );
    }

    private UserResponse creditReceiver(UserResponse receiver, BigDecimal usdAmount) {
        return userClient.updateBalance(
                receiver.id(),
                new UpdateUserBalanceRequest(receiver.brlBalance(), receiver.usdBalance().add(usdAmount))
        );
    }

    private void compensateSenderIfNeeded(UserResponse sender, UserResponse updatedSender) {
        if (updatedSender != null) {
            userClient.updateBalance(sender.id(), new UpdateUserBalanceRequest(sender.brlBalance(), sender.usdBalance()));
        }
    }

    private void updateStatus(Remittance remittance, RemittanceStatus status, String failureReason) {
        remittance.setStatus(status);
        remittance.setFailureReason(failureReason);
        remittanceRepository.update(remittance);
    }

    private BigDecimal findExchangeRate(CreateRemittanceRequest request) {
        String quotationDate = "'" + request.quotationDate().format(PTAX_DATE_FORMATTER) + "'";
        PtaxQuotationResponse response = ptaxClient.findDollarQuotation(quotationDate, 100, "json");

        return response.value().stream()
                .map(PtaxQuotation::cotacaoCompra)
                .filter(Objects::nonNull)
                .findFirst()
                .map(exchangeRate -> {
                    lastExchangeRate.set(exchangeRate);
                    return exchangeRate;
                })
                .orElseGet(() -> cachedExchangeRate(request));
    }

    private BigDecimal cachedExchangeRate(CreateRemittanceRequest request) {
        BigDecimal exchangeRate = lastExchangeRate.get();

        if (exchangeRate == null) {
            throw new RemittanceException("Dollar quotation not found for date: " + request.quotationDate());
        }

        return exchangeRate;
    }
}
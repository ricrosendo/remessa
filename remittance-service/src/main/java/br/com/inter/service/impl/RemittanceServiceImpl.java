package br.com.inter.service.impl;

import br.com.inter.client.UserClient;
import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.RemittanceStatus;
import br.com.inter.exception.RemittanceException;
import br.com.inter.model.Remittance;
import br.com.inter.repository.RemittanceRepository;
import br.com.inter.service.ExchangeRateService;
import br.com.inter.service.RemittanceService;
import br.com.inter.validator.DailyRemittanceLimitValidator;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Singleton
public class RemittanceServiceImpl implements RemittanceService {

    private final UserClient userClient;
    private final RemittanceRepository remittanceRepository;
    private final ExchangeRateService exchangeRateService;
    private final DailyRemittanceLimitValidator dailyRemittanceLimitValidator;

    public RemittanceServiceImpl(
            UserClient userClient,
            RemittanceRepository remittanceRepository,
            ExchangeRateService exchangeRateService,
            DailyRemittanceLimitValidator dailyRemittanceLimitValidator
    ) {
        this.userClient = userClient;
        this.remittanceRepository = remittanceRepository;
        this.exchangeRateService = exchangeRateService;
        this.dailyRemittanceLimitValidator = dailyRemittanceLimitValidator;
    }

    @Override
    public RemittanceResponse create(CreateRemittanceRequest request) {
        if (request.senderUserId().equals(request.receiverUserId())) {
            throw new RemittanceException("Sender and receiver users must be different");
        }

        UserResponse sender = userClient.findById(request.senderUserId());
        UserResponse receiver = userClient.findById(request.receiverUserId());
        dailyRemittanceLimitValidator.validate(sender, request);
        BigDecimal exchangeRate = exchangeRateService.findExchangeRate(request.quotationDate());

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
}
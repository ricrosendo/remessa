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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Singleton
public class RemittanceServiceImpl implements RemittanceService {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemittanceServiceImpl.class);

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
        LOGGER.info("Starting remittance creation. senderUserId={}, receiverUserId={}, quotationDate={}", request.senderUserId(), request.receiverUserId(), request.quotationDate());

        if (request.senderUserId().equals(request.receiverUserId())) {
            LOGGER.warn("Remittance rejected because sender and receiver are the same. userId={}", request.senderUserId());
            throw new RemittanceException("Sender and receiver users must be different");
        }

        LOGGER.debug("Fetching sender user. senderUserId={}", request.senderUserId());
        UserResponse sender = userClient.findById(request.senderUserId());
        LOGGER.debug("Fetching receiver user. receiverUserId={}", request.receiverUserId());
        UserResponse receiver = userClient.findById(request.receiverUserId());

        dailyRemittanceLimitValidator.validate(sender, request);
        BigDecimal exchangeRate = exchangeRateService.findExchangeRate(request.quotationDate());

        if (sender.brlBalance().compareTo(request.brlAmount()) < 0) {
            LOGGER.warn("Remittance rejected because sender has insufficient BRL balance. senderUserId={}", sender.id());
            throw new RemittanceException("Insufficient BRL balance for remittance");
        }

        BigDecimal usdAmount = request.brlAmount().divide(exchangeRate, 2, RoundingMode.HALF_UP);
        LOGGER.info("Remittance amounts calculated. senderUserId={}, receiverUserId={}, quotationDate={}", sender.id(), receiver.id(), request.quotationDate());
        Remittance remittance = createPendingRemittance(request, exchangeRate, usdAmount);

        UserResponse updatedSender = null;

        try {
            updatedSender = debitSender(sender, request);
            updateStatus(remittance, RemittanceStatus.SENDER_DEBITED, null);

            UserResponse updatedReceiver = creditReceiver(receiver, usdAmount);
            updateStatus(remittance, RemittanceStatus.COMPLETED, null);
            remittance.setCompletedAt(LocalDateTime.now());
            remittanceRepository.update(remittance);
            LOGGER.info("Remittance completed successfully. remittanceId={}, senderUserId={}, receiverUserId={}", remittance.getId(), sender.id(), receiver.id());

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
            LOGGER.error("Remittance processing failed. remittanceId={}, senderUserId={}, receiverUserId={}, message={}", remittance.getId(), sender.id(), receiver.id(), exception.getMessage(), exception);
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
        Remittance savedRemittance = remittanceRepository.save(remittance);
        LOGGER.info("Pending remittance persisted. remittanceId={}, senderUserId={}, receiverUserId={}", savedRemittance.getId(), request.senderUserId(), request.receiverUserId());
        return savedRemittance;
    }

    private UserResponse debitSender(UserResponse sender, CreateRemittanceRequest request) {
        LOGGER.info("Debiting sender balance. senderUserId={}", sender.id());
        UserResponse updatedSender = userClient.updateBalance(
                sender.id(),
                new UpdateUserBalanceRequest(sender.brlBalance().subtract(request.brlAmount()), sender.usdBalance())
        );
        LOGGER.info("Sender balance debited successfully. senderUserId={}", sender.id());
        return updatedSender;
    }

    private UserResponse creditReceiver(UserResponse receiver, BigDecimal usdAmount) {
        LOGGER.info("Crediting receiver balance. receiverUserId={}", receiver.id());
        UserResponse updatedReceiver = userClient.updateBalance(
                receiver.id(),
                new UpdateUserBalanceRequest(receiver.brlBalance(), receiver.usdBalance().add(usdAmount))
        );
        LOGGER.info("Receiver balance credited successfully. receiverUserId={}", receiver.id());
        return updatedReceiver;
    }

    private void compensateSenderIfNeeded(UserResponse sender, UserResponse updatedSender) {
        if (updatedSender != null) {
            LOGGER.warn("Compensating sender balance after remittance failure. senderUserId={}", sender.id());
            userClient.updateBalance(sender.id(), new UpdateUserBalanceRequest(sender.brlBalance(), sender.usdBalance()));
            LOGGER.warn("Sender balance compensated successfully. senderUserId={}", sender.id());
        }
    }

    private void updateStatus(Remittance remittance, RemittanceStatus status, String failureReason) {
        remittance.setStatus(status);
        remittance.setFailureReason(failureReason);
        remittanceRepository.update(remittance);
        LOGGER.info("Remittance status updated. remittanceId={}, status={}", remittance.getId(), status);
    }
}
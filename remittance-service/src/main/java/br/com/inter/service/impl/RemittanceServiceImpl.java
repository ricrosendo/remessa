package br.com.inter.service.impl;

import br.com.inter.client.PtaxClient;
import br.com.inter.client.UserClient;
import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.PtaxQuotation;
import br.com.inter.dto.PtaxQuotationResponse;
import br.com.inter.dto.RemittanceResponse;
import br.com.inter.dto.UpdateUserBalanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.exception.RemittanceException;
import br.com.inter.service.RemittanceService;
import jakarta.inject.Singleton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Singleton
public class RemittanceServiceImpl implements RemittanceService {

    private static final DateTimeFormatter PTAX_DATE_FORMATTER = DateTimeFormatter.ofPattern("MM-dd-yyyy");

    private final UserClient userClient;
    private final PtaxClient ptaxClient;

    public RemittanceServiceImpl(UserClient userClient, PtaxClient ptaxClient) {
        this.userClient = userClient;
        this.ptaxClient = ptaxClient;
    }

    @Override
    public RemittanceResponse create(CreateRemittanceRequest request) {
        if (request.senderUserId().equals(request.receiverUserId())) {
            throw new RemittanceException("Sender and receiver users must be different");
        }

        UserResponse sender = userClient.findById(request.senderUserId());
        UserResponse receiver = userClient.findById(request.receiverUserId());
        BigDecimal exchangeRate = findExchangeRate(request);

        if (sender.brlBalance().compareTo(request.brlAmount()) < 0) {
            throw new RemittanceException("Insufficient BRL balance for remittance");
        }

        BigDecimal usdAmount = request.brlAmount().divide(exchangeRate, 2, RoundingMode.HALF_UP);

        UserResponse updatedSender = userClient.updateBalance(
                sender.id(),
                new UpdateUserBalanceRequest(sender.brlBalance().subtract(request.brlAmount()), sender.usdBalance())
        );
        UserResponse updatedReceiver = userClient.updateBalance(
                receiver.id(),
                new UpdateUserBalanceRequest(receiver.brlBalance(), receiver.usdBalance().add(usdAmount))
        );

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
    }

    private BigDecimal findExchangeRate(CreateRemittanceRequest request) {
        String quotationDate = "'" + request.quotationDate().format(PTAX_DATE_FORMATTER) + "'";
        PtaxQuotationResponse response = ptaxClient.findDollarQuotation(quotationDate, 100, "json");

        return response.value().stream()
                .map(PtaxQuotation::cotacaoCompra)
                .filter(Objects::nonNull)
                .findFirst()
                .orElseThrow(() -> new RemittanceException("Dollar quotation not found for date: " + request.quotationDate()));
    }
}

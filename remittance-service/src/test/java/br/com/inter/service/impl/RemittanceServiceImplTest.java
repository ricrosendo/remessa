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
import br.com.inter.validator.DailyRemittanceLimitValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class RemittanceServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private PtaxClient ptaxClient;

    @Mock
    private RemittanceRepository remittanceRepository;

    private RemittanceServiceImpl remittanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(remittanceRepository.save(any(Remittance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(remittanceRepository.update(any(Remittance.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(any(), any(), any())).thenReturn(BigDecimal.ZERO);
        remittanceService = new RemittanceServiceImpl(userClient, remittanceRepository, new PtaxExchangeRateService(ptaxClient), new DailyRemittanceLimitValidator(remittanceRepository));
    }

    @Test
    void shouldCreateRemittance() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));
        UserResponse sender = user(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        UserResponse receiver = user(receiverId, BigDecimal.ZERO, BigDecimal.TEN);
        UserResponse updatedSender = user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO);
        UserResponse updatedReceiver = user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(110));

        when(userClient.findById(senderId)).thenReturn(sender);
        when(userClient.findById(receiverId)).thenReturn(receiver);
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO))).thenReturn(updatedSender);
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(110)))).thenReturn(updatedReceiver);

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(senderId, response.senderUserId());
        assertEquals(receiverId, response.receiverUserId());
        assertEquals(BigDecimal.valueOf(500), response.brlAmount());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.usdAmount());
        assertEquals(BigDecimal.valueOf(5), response.exchangeRate());

        ArgumentCaptor<UpdateUserBalanceRequest> senderBalanceCaptor = ArgumentCaptor.forClass(UpdateUserBalanceRequest.class);
        verify(userClient).updateBalance(eq(senderId), senderBalanceCaptor.capture());
        assertEquals(BigDecimal.valueOf(500), senderBalanceCaptor.getValue().brlBalance());

        ArgumentCaptor<Remittance> remittanceCaptor = ArgumentCaptor.forClass(Remittance.class);
        verify(remittanceRepository).save(remittanceCaptor.capture());
        verify(remittanceRepository, org.mockito.Mockito.atLeastOnce()).update(remittanceCaptor.getValue());
        assertEquals(RemittanceStatus.COMPLETED, remittanceCaptor.getValue().getStatus());
    }

    @Test
    void shouldNotCreateRemittanceForSameUser() {
        UUID userId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(userId, userId, BigDecimal.TEN, LocalDate.of(2025, 1, 30));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Sender and receiver users must be different", exception.getMessage());
        verify(userClient, never()).findById(userId);
    }

    @Test
    void shouldNotCreateRemittanceWhenBalanceIsInsufficient() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(100), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Insufficient BRL balance for remittance", exception.getMessage());
        verify(userClient, never()).updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void shouldNotCreateRemittanceWhenQuotationIsNotFound() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of()));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Dollar quotation not found for date: 2025-01-30", exception.getMessage());
        verify(userClient, never()).updateBalance(any(), any());
    }

    @Test
    void shouldCompensateSenderAndPersistCompensatedStatusWhenReceiverCreditFails() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));
        UserResponse sender = user(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO);
        UserResponse receiver = user(receiverId, BigDecimal.ZERO, BigDecimal.TEN);

        when(userClient.findById(senderId)).thenReturn(sender);
        when(userClient.findById(receiverId)).thenReturn(receiver);
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        doThrow(new RuntimeException("receiver unavailable"))
                .when(userClient).updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(110).setScale(2)));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Remittance transaction failed: receiver unavailable", exception.getMessage());
        verify(userClient).updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(1000), BigDecimal.ZERO));

        ArgumentCaptor<Remittance> remittanceCaptor = ArgumentCaptor.forClass(Remittance.class);
        verify(remittanceRepository).save(remittanceCaptor.capture());
        assertEquals(RemittanceStatus.COMPENSATED, remittanceCaptor.getValue().getStatus());
        assertEquals("receiver unavailable", remittanceCaptor.getValue().getFailureReason());
    }

    @Test
    void shouldCreateRemittanceWhenSenderHasExactBalance() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(BigDecimal.valueOf(100).setScale(2), response.usdAmount());
        verify(userClient).updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.ZERO));
    }

    @Test
    void shouldRoundConvertedUsdAmountUsingHalfUp() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(100), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(100), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(3)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(33.33))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(33.33)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(BigDecimal.valueOf(33.33), response.usdAmount());
        verify(userClient).updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(33.33)));
    }

    @Test
    void shouldUseFirstNonNullQuotation() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(100), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(100), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(null), new PtaxQuotation(BigDecimal.valueOf(4)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(25).setScale(2))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(25).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(BigDecimal.valueOf(4), response.exchangeRate());
        assertEquals(BigDecimal.valueOf(25).setScale(2), response.usdAmount());
    }

    @Test
    void shouldNotCreateRemittanceWhenIndividualDailyLimitIsExceeded() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(1000), quotationDate);

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(20000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(9500));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Daily remittance limit exceeded for user type INDIVIDUAL", exception.getMessage());
        verify(ptaxClient, never()).findDollarQuotation(any(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(userClient, never()).updateBalance(any(), any());
    }

    @Test
    void shouldNotCreateRemittanceWhenCompanyDailyLimitIsExceeded() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(1000), quotationDate);

        when(userClient.findById(senderId)).thenReturn(company(senderId, BigDecimal.valueOf(60000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(49500));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Daily remittance limit exceeded for user type COMPANY", exception.getMessage());
        verify(ptaxClient, never()).findDollarQuotation(any(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(userClient, never()).updateBalance(any(), any());
    }

    @Test
    void shouldCreateRemittanceWhenIndividualReachesExactDailyLimit() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), quotationDate);

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(9500));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(BigDecimal.valueOf(500), response.brlAmount());
    }

    @Test
    void shouldCreateRemittanceWhenCompanyReachesExactDailyLimit() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), quotationDate);

        when(userClient.findById(senderId)).thenReturn(company(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(49500));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(company(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(BigDecimal.valueOf(500), response.brlAmount());
    }

    @Test
    void shouldNotCreateRemittanceWhenSenderUserTypeIsUnsupported() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(userWithType(senderId, "UNKNOWN", BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.ZERO));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.create(request));

        assertEquals("Unsupported user type for remittance: UNKNOWN", exception.getMessage());
        verify(remittanceRepository, never()).sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(any(), any(), any());
        verify(ptaxClient, never()).findDollarQuotation(any(), org.mockito.ArgumentMatchers.anyInt(), any());
        verify(userClient, never()).updateBalance(any(), any());
    }

    @Test
    void shouldCreateRemittanceFromIndividualToCompany() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(user(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(company(receiverId, BigDecimal.ZERO, BigDecimal.TEN));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(user(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(110).setScale(2))))
                .thenReturn(company(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(110).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(senderId, response.senderUserId());
        assertEquals(receiverId, response.receiverUserId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.usdAmount());
    }

    @Test
    void shouldCreateRemittanceFromCompanyToIndividual() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        CreateRemittanceRequest request = new CreateRemittanceRequest(senderId, receiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 30));

        when(userClient.findById(senderId)).thenReturn(company(senderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(receiverId)).thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.TEN));
        when(ptaxClient.findDollarQuotation("'01-30-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(userClient.updateBalance(senderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(company(senderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(receiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(110).setScale(2))))
                .thenReturn(user(receiverId, BigDecimal.ZERO, BigDecimal.valueOf(110).setScale(2)));

        RemittanceResponse response = remittanceService.create(request);

        assertEquals(senderId, response.senderUserId());
        assertEquals(receiverId, response.receiverUserId());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.usdAmount());
    }

    @Test
    void shouldUseCachedExchangeRateWhenQuotationIsNotFoundAfterPreviousSuccessfulQuotation() {
        UUID firstSenderId = UUID.randomUUID();
        UUID firstReceiverId = UUID.randomUUID();
        UUID secondSenderId = UUID.randomUUID();
        UUID secondReceiverId = UUID.randomUUID();
        CreateRemittanceRequest firstRequest = new CreateRemittanceRequest(firstSenderId, firstReceiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 1, 31));
        CreateRemittanceRequest secondRequest = new CreateRemittanceRequest(secondSenderId, secondReceiverId, BigDecimal.valueOf(500), LocalDate.of(2025, 2, 1));

        when(userClient.findById(firstSenderId)).thenReturn(user(firstSenderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(firstReceiverId)).thenReturn(user(firstReceiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(userClient.findById(secondSenderId)).thenReturn(user(secondSenderId, BigDecimal.valueOf(1000), BigDecimal.ZERO));
        when(userClient.findById(secondReceiverId)).thenReturn(user(secondReceiverId, BigDecimal.ZERO, BigDecimal.ZERO));
        when(ptaxClient.findDollarQuotation("'01-31-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of(new PtaxQuotation(BigDecimal.valueOf(5)))));
        when(ptaxClient.findDollarQuotation("'02-01-2025'", 100, "json"))
                .thenReturn(new PtaxQuotationResponse(List.of()));
        when(userClient.updateBalance(firstSenderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(user(firstSenderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(firstReceiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))))
                .thenReturn(user(firstReceiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2)));
        when(userClient.updateBalance(secondSenderId, new UpdateUserBalanceRequest(BigDecimal.valueOf(500), BigDecimal.ZERO)))
                .thenReturn(user(secondSenderId, BigDecimal.valueOf(500), BigDecimal.ZERO));
        when(userClient.updateBalance(secondReceiverId, new UpdateUserBalanceRequest(BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2))))
                .thenReturn(user(secondReceiverId, BigDecimal.ZERO, BigDecimal.valueOf(100).setScale(2)));

        RemittanceResponse firstResponse = remittanceService.create(firstRequest);
        RemittanceResponse secondResponse = remittanceService.create(secondRequest);

        assertEquals(BigDecimal.valueOf(5), firstResponse.exchangeRate());
        assertEquals(BigDecimal.valueOf(5), secondResponse.exchangeRate());
        assertEquals(BigDecimal.valueOf(100).setScale(2), secondResponse.usdAmount());
    }

    @Test
    void shouldListRemittancesByFilters() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        LocalDate startDate = LocalDate.of(2025, 1, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);
        Remittance remittance = remittance(senderId, receiverId, BigDecimal.valueOf(500), BigDecimal.valueOf(100).setScale(2), BigDecimal.valueOf(5), LocalDate.of(2025, 1, 30));

        when(remittanceRepository.findByFilters(startDate, endDate, senderId)).thenReturn(List.of(remittance));

        List<RemittanceResponse> response = remittanceService.list(startDate, endDate, senderId);

        assertEquals(1, response.size());
        assertEquals(senderId, response.getFirst().senderUserId());
        assertEquals(receiverId, response.getFirst().receiverUserId());
        assertEquals(BigDecimal.valueOf(500), response.getFirst().brlAmount());
        assertEquals(BigDecimal.valueOf(100).setScale(2), response.getFirst().usdAmount());
        assertEquals(BigDecimal.valueOf(5), response.getFirst().exchangeRate());
        assertEquals(LocalDate.of(2025, 1, 30), response.getFirst().quotationDate());
        verify(remittanceRepository).findByFilters(startDate, endDate, senderId);
    }

    @Test
    void shouldListRemittancesWithoutFilters() {
        UUID senderId = UUID.randomUUID();
        UUID receiverId = UUID.randomUUID();
        Remittance remittance = remittance(senderId, receiverId, BigDecimal.valueOf(250), BigDecimal.valueOf(50).setScale(2), BigDecimal.valueOf(5), LocalDate.of(2025, 2, 1));

        when(remittanceRepository.findByFilters(null, null, null)).thenReturn(List.of(remittance));

        List<RemittanceResponse> response = remittanceService.list(null, null, null);

        assertEquals(1, response.size());
        assertEquals(senderId, response.getFirst().senderUserId());
        assertEquals(receiverId, response.getFirst().receiverUserId());
        verify(remittanceRepository).findByFilters(null, null, null);
    }

    @Test
    void shouldNotListRemittancesWhenStartDateIsAfterEndDate() {
        LocalDate startDate = LocalDate.of(2025, 2, 1);
        LocalDate endDate = LocalDate.of(2025, 1, 31);

        RemittanceException exception = assertThrows(RemittanceException.class, () -> remittanceService.list(startDate, endDate, null));

        assertEquals("Start date must be before or equal to end date", exception.getMessage());
        verify(remittanceRepository, never()).findByFilters(any(), any(), any());
    }

    private Remittance remittance(UUID senderId, UUID receiverId, BigDecimal brlAmount, BigDecimal usdAmount, BigDecimal exchangeRate, LocalDate quotationDate) {
        Remittance remittance = new Remittance();
        remittance.setSenderUserId(senderId);
        remittance.setReceiverUserId(receiverId);
        remittance.setBrlAmount(brlAmount);
        remittance.setUsdAmount(usdAmount);
        remittance.setExchangeRate(exchangeRate);
        remittance.setQuotationDate(quotationDate);
        remittance.setStatus(RemittanceStatus.COMPLETED);
        remittance.setCreatedAt(java.time.LocalDateTime.now());
        return remittance;
    }
    private UserResponse user(UUID id, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "User", "user@email.com", "INDIVIDUAL", "12345678901", null, brlBalance, usdBalance);
    }

    private UserResponse userWithType(UUID id, String type, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "User", "user@email.com", type, "12345678901", null, brlBalance, usdBalance);
    }

    private UserResponse company(UUID id, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "Company", "company@email.com", "COMPANY", null, "12345678000199", brlBalance, usdBalance);
    }
}
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemittanceServiceImplTest {

    @Mock
    private UserClient userClient;

    @Mock
    private PtaxClient ptaxClient;

    private RemittanceServiceImpl remittanceService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        remittanceService = new RemittanceServiceImpl(userClient, ptaxClient);
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
    }

    private UserResponse user(UUID id, BigDecimal brlBalance, BigDecimal usdBalance) {
        return new UserResponse(id, "User", "user@email.com", "INDIVIDUAL", "12345678901", null, brlBalance, usdBalance);
    }
}

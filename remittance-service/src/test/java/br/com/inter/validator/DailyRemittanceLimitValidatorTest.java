package br.com.inter.validator;

import br.com.inter.dto.CreateRemittanceRequest;
import br.com.inter.dto.UserResponse;
import br.com.inter.enums.RemittanceStatus;
import br.com.inter.exception.RemittanceException;
import br.com.inter.repository.RemittanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DailyRemittanceLimitValidatorTest {

    @Mock
    private RemittanceRepository remittanceRepository;

    private DailyRemittanceLimitValidator validator;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        validator = new DailyRemittanceLimitValidator(remittanceRepository);
    }

    @Test
    void shouldAllowIndividualWhenDailyLimitIsNotExceeded() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "INDIVIDUAL");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(500), quotationDate);

        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(9000));

        assertDoesNotThrow(() -> validator.validate(sender, request));
    }

    @Test
    void shouldAllowIndividualWhenDailyLimitIsReachedExactly() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "INDIVIDUAL");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(500), quotationDate);

        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(9500));

        assertDoesNotThrow(() -> validator.validate(sender, request));
    }

    @Test
    void shouldRejectIndividualWhenDailyLimitIsExceeded() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "INDIVIDUAL");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(501), quotationDate);

        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(9500));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> validator.validate(sender, request));

        assertEquals("Daily remittance limit exceeded for user type INDIVIDUAL", exception.getMessage());
    }

    @Test
    void shouldAllowCompanyWhenDailyLimitIsReachedExactly() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "COMPANY");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(500), quotationDate);

        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(49500));

        assertDoesNotThrow(() -> validator.validate(sender, request));
    }

    @Test
    void shouldRejectCompanyWhenDailyLimitIsExceeded() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "COMPANY");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(501), quotationDate);

        when(remittanceRepository.sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED))
                .thenReturn(BigDecimal.valueOf(49500));

        RemittanceException exception = assertThrows(RemittanceException.class, () -> validator.validate(sender, request));

        assertEquals("Daily remittance limit exceeded for user type COMPANY", exception.getMessage());
    }

    @Test
    void shouldRejectUnsupportedUserTypeWithoutQueryingRepository() {
        UUID senderId = UUID.randomUUID();
        LocalDate quotationDate = LocalDate.of(2025, 1, 30);
        UserResponse sender = user(senderId, "UNKNOWN");
        CreateRemittanceRequest request = request(senderId, BigDecimal.valueOf(500), quotationDate);

        RemittanceException exception = assertThrows(RemittanceException.class, () -> validator.validate(sender, request));

        assertEquals("Unsupported user type for remittance: UNKNOWN", exception.getMessage());
        verify(remittanceRepository, never()).sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(senderId, quotationDate, RemittanceStatus.COMPLETED);
    }

    private CreateRemittanceRequest request(UUID senderId, BigDecimal brlAmount, LocalDate quotationDate) {
        return new CreateRemittanceRequest(senderId, UUID.randomUUID(), brlAmount, quotationDate);
    }

    private UserResponse user(UUID id, String type) {
        return new UserResponse(id, "User", "user@email.com", type, "12345678901", null, BigDecimal.ZERO, BigDecimal.ZERO);
    }
}

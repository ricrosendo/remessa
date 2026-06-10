package br.com.inter.model;

import br.com.inter.enums.RemittanceStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "remittances")
public class Remittance {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID senderUserId;

    @Column(nullable = false)
    private UUID receiverUserId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal brlAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal usdAmount;

    @Column(nullable = false, precision = 19, scale = 6)
    private BigDecimal exchangeRate;

    @Column(nullable = false)
    private LocalDate quotationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RemittanceStatus status;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime completedAt;

    @Column(length = 1000)
    private String failureReason;
}

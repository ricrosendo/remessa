package br.com.inter.repository;

import br.com.inter.enums.RemittanceStatus;
import br.com.inter.model.Remittance;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.CrudRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface RemittanceRepository extends CrudRepository<Remittance, UUID> {

    @Query("SELECT COALESCE(SUM(r.brlAmount), 0) FROM Remittance r WHERE r.senderUserId = :senderUserId AND r.quotationDate = :quotationDate AND r.status = :status")
    BigDecimal sumBrlAmountBySenderUserIdAndQuotationDateAndStatus(UUID senderUserId, LocalDate quotationDate, RemittanceStatus status);

    @Query("""
            SELECT r FROM Remittance r
            WHERE (:startDate IS NULL OR r.quotationDate >= :startDate)
            AND (:endDate IS NULL OR r.quotationDate <= :endDate)
            AND (:userId IS NULL OR r.senderUserId = :userId OR r.receiverUserId = :userId)
            ORDER BY r.quotationDate DESC, r.createdAt DESC
            """)
    List<Remittance> findByFilters(LocalDate startDate, LocalDate endDate, UUID userId);
}

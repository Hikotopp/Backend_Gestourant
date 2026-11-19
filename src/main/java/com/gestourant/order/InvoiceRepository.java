package com.gestourant.order; import jakarta.persistence.LockModeType; import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"order", "order.createdBy", "order.items", "order.items.product"})
    @Query("select invoice from Invoice invoice where invoice.id = :id")
    java.util.Optional<Invoice> findForUpdateById(@Param("id") Long id);

    List<Invoice> findByIssuedAtBetween(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"order", "order.table", "order.createdBy", "order.items", "order.items.product"})
    List<Invoice> findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtAsc(
        LocalDateTime from,
        LocalDateTime to
    );

    @EntityGraph(attributePaths = {"order", "order.table", "order.createdBy", "order.items", "order.items.product"})
    List<Invoice> findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
        LocalDateTime from,
        LocalDateTime to,
        Long employeeId
    );
}

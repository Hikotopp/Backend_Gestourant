package com.gestourant.order; import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    List<Invoice> findByIssuedAtBetween(LocalDateTime from, LocalDateTime to);

    @EntityGraph(attributePaths = {"order", "order.table", "order.createdBy"})
    List<Invoice> findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtAsc(
        LocalDateTime from,
        LocalDateTime to
    );

    @EntityGraph(attributePaths = {"order", "order.table", "order.createdBy"})
    List<Invoice> findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
        LocalDateTime from,
        LocalDateTime to,
        Long employeeId
    );
}

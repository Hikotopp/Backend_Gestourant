package com.gestourant.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductReturnRepository extends JpaRepository<ProductReturn, Long> {
    @EntityGraph(attributePaths = {"items", "items.orderItem", "items.product", "processedBy"})
    List<ProductReturn> findByInvoiceIdOrderByReturnedAtDesc(Long invoiceId);
}

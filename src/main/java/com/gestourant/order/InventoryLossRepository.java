package com.gestourant.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface InventoryLossRepository extends JpaRepository<InventoryLoss, Long> {
    @EntityGraph(attributePaths = {"reportedBy"})
    List<InventoryLoss> findByReportedAtGreaterThanEqualAndReportedAtLessThanOrderByReportedAtDesc(
        LocalDateTime from,
        LocalDateTime to
    );

    @EntityGraph(attributePaths = {"reportedBy"})
    List<InventoryLoss> findByReportedAtGreaterThanEqualAndReportedAtLessThanAndReportedBy_IdOrderByReportedAtDesc(
        LocalDateTime from,
        LocalDateTime to,
        Long userId
    );
}

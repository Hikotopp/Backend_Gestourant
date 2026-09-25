package com.gestourant.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GuestOrderRequestRepository extends JpaRepository<GuestOrderRequest, Long> {
    boolean existsByTableId(Long tableId);
    boolean existsByTableIdAndStatus(Long tableId, GuestRequestStatus status);
    boolean existsByTableIdAndStatusIn(Long tableId, List<GuestRequestStatus> statuses);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    List<GuestOrderRequest> findByStatusOrderByCreatedAtAsc(GuestRequestStatus status);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    List<GuestOrderRequest> findByStatusInOrderByCreatedAtAsc(List<GuestRequestStatus> statuses);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    List<GuestOrderRequest> findByTableQrTokenOrderByCreatedAtDesc(String qrToken);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    List<GuestOrderRequest> findTop15ByTableQrTokenAndGuestSessionHashOrderByCreatedAtDesc(String qrToken, String guestSessionHash);

    @Override
    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    Optional<GuestOrderRequest> findById(Long id);
}

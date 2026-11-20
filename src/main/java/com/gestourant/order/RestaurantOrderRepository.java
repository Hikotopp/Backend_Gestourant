package com.gestourant.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RestaurantOrderRepository extends JpaRepository<RestaurantOrder, Long> {
    @Override
    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    Optional<RestaurantOrder> findById(Long id);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    Optional<RestaurantOrder> findByTableIdAndStatus(Long tableId, OrderStatus status);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    List<RestaurantOrder> findByStatusOrderByOpenedAtAsc(OrderStatus status);

    @EntityGraph(attributePaths = {"table", "items", "items.product"})
    @Query("""
        select distinct o
        from RestaurantOrder o
        join o.items item
        where o.status = :status and item.kitchenBatchId = :batchId
        """)
    Optional<RestaurantOrder> findKitchenBatchOrder(
        @Param("status") OrderStatus status,
        @Param("batchId") String batchId
    );
}

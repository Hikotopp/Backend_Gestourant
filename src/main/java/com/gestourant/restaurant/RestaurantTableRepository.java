package com.gestourant.restaurant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RestaurantTableRepository extends JpaRepository<RestaurantTable, Long> {
    boolean existsByTableNumber(Integer tableNumber);
    Optional<RestaurantTable> findByQrToken(String qrToken);
}

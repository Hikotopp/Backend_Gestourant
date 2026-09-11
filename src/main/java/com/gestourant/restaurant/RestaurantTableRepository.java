package com.gestourant.restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
public interface RestaurantTableRepository extends JpaRepository<RestaurantTable,Long>{ boolean existsByTableNumber(Integer tableNumber); }

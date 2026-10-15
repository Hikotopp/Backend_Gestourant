package com.gestourant.catalog.adapter.out.persistence;

import com.gestourant.catalog.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductJpaRepository extends JpaRepository<Product, Long> {
    List<Product> findByActiveTrueAndStockGreaterThanOrderByCategoryAscNameAsc(int stock);
}

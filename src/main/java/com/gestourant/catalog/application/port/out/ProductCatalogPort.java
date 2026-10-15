package com.gestourant.catalog.application.port.out;

import com.gestourant.catalog.Product;

import java.util.List;
import java.util.Optional;

/**
 * Outbound port for product persistence. Application services depend on this
 * contract rather than on Spring Data or a particular database.
 */
public interface ProductCatalogPort {
    List<Product> findAll();

    List<Product> findAvailable();

    Optional<Product> findById(Long id);

    Product save(Product product);

    void delete(Product product);
}

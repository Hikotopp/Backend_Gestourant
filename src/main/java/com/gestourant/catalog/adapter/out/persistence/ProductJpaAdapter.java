package com.gestourant.catalog.adapter.out.persistence;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/** Spring Data implementation of the product persistence port. */
@Component
public class ProductJpaAdapter implements ProductCatalogPort {
    private final ProductJpaRepository products;

    public ProductJpaAdapter(ProductJpaRepository products) {
        this.products = products;
    }

    @Override
    public List<Product> findAll() {
        return products.findAll();
    }

    @Override
    public List<Product> findAvailable() {
        return products.findByActiveTrueAndStockGreaterThanOrderByCategoryAscNameAsc(0);
    }

    @Override
    public Optional<Product> findById(Long id) {
        return products.findById(id);
    }

    @Override
    public Product save(Product product) {
        return products.save(product);
    }

    @Override
    public void delete(Product product) {
        products.delete(product);
    }
}

package com.gestourant.catalog.application;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;

import java.math.BigDecimal;
import java.util.List;

/** Application use cases for the product catalogue. */
public class CatalogService {
    private final ProductCatalogPort products;

    public CatalogService(ProductCatalogPort products) {
        this.products = products;
    }

    public List<Product> list() {
        return products.findAll();
    }

    public Product create(ProductCommand command) {
        return products.save(new Product(
            command.name(),
            command.description(),
            command.category(),
            command.imageUrl(),
            command.price(),
            command.stock()
        ));
    }

    public Product update(Long id, ProductCommand command) {
        Product product = getRequired(id);
        product.update(
            command.name(),
            command.description(),
            command.category(),
            command.imageUrl(),
            command.price(),
            command.stock(),
            command.active()
        );
        return products.save(product);
    }

    public void delete(Long id) {
        products.delete(getRequired(id));
    }

    private Product getRequired(Long id) {
        return products.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
    }

    public record ProductCommand(
        String name,
        String description,
        String category,
        String imageUrl,
        BigDecimal price,
        Integer stock,
        boolean active
    ) {}
}

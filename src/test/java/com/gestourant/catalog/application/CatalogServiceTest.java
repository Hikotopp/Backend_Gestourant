package com.gestourant.catalog.application;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CatalogServiceTest {
    private final ProductCatalogPort products = mock(ProductCatalogPort.class);
    private final CatalogService service = new CatalogService(products);

    @Test
    void createsAProductThroughThePersistencePort() {
        CatalogService.ProductCommand command = command("Limonada", true);
        Product product = new Product("Limonada", "Natural", "BEBIDA", "https://example.com/lemonade.jpg", new BigDecimal("6000"), 8);
        when(products.save(any(Product.class))).thenReturn(product);

        Product created = service.create(command);

        assertEquals("Limonada", created.getName());
        verify(products).save(argThat(saved -> saved.getName().equals("Limonada") && saved.isActive()));
    }

    @Test
    void updatesAnExistingProductThroughThePersistencePort() {
        Product existing = new Product("Limonada", "Natural", "BEBIDA", "https://example.com/old.jpg", new BigDecimal("6000"), 8);
        when(products.findById(5L)).thenReturn(Optional.of(existing));
        when(products.save(existing)).thenReturn(existing);

        Product updated = service.update(5L, command("Limonada de coco", false));

        assertEquals("Limonada de coco", updated.getName());
        assertEquals(false, updated.isActive());
        verify(products).save(existing);
    }

    @Test
    void rejectsUpdatingAnUnknownProduct() {
        when(products.findById(99L)).thenReturn(Optional.empty());

        assertThrows(IllegalArgumentException.class, () -> service.update(99L, command("No existe", true)));
    }

    private CatalogService.ProductCommand command(String name, boolean active) {
        return new CatalogService.ProductCommand(
            name,
            "Bebida preparada al momento",
            "BEBIDA",
            "https://example.com/lemonade.jpg",
            new BigDecimal("6000"),
            8,
            active
        );
    }
}

package com.gestourant.catalog.adapter.in.web;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.CatalogService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/** HTTP adapter for the catalogue use cases. */
@RestController
@RequestMapping("/api/products")
public class ProductController {
    private final CatalogService catalog;

    public ProductController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<ProductResponse> list() {
        return catalog.list().stream().map(ProductResponse::from).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public ProductResponse create(@Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(catalog.create(request.toCommand()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @Transactional
    public ProductResponse update(@PathVariable Long id, @Valid @RequestBody ProductRequest request) {
        return ProductResponse.from(catalog.update(id, request.toCommand()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void delete(@PathVariable Long id) {
        catalog.delete(id);
    }

    public record ProductRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Size(max = 500) String description,
        @NotBlank @Size(max = 30) String category,
        @Size(max = 1000)
        @Pattern(regexp = "^$|^https://.+", message = "La imagen debe usar una dirección HTTPS") String imageUrl,
        @NotNull @DecimalMin("0.01") BigDecimal price,
        @NotNull @Min(0) Integer stock,
        boolean active
    ) {
        CatalogService.ProductCommand toCommand() {
            return new CatalogService.ProductCommand(name, description, category, imageUrl, price, stock, active);
        }
    }

    public record ProductResponse(
        Long id,
        String name,
        String description,
        String category,
        String imageUrl,
        BigDecimal price,
        Integer stock,
        boolean active
    ) {
        static ProductResponse from(Product product) {
            return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getCategory(),
                product.getImageUrl(),
                product.getPrice(),
                product.getStock(),
                product.isActive()
            );
        }
    }
}

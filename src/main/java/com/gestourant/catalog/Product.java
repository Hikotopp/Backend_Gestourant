package com.gestourant.catalog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 500)
    private String description;

    @Column(nullable = false, length = 30)
    private String category;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost = BigDecimal.ZERO;

    @Column(nullable = false)
    private Integer stock;

    @Column(nullable = false)
    private boolean active = true;

    protected Product() {}

    public Product(String name, String description, String category, BigDecimal price, Integer stock) {
        this(name, description, category, null, price, BigDecimal.ZERO, stock);
    }

    public Product(String name, String description, String category, String imageUrl, BigDecimal price, Integer stock) {
        this(name, description, category, imageUrl, price, BigDecimal.ZERO, stock);
    }

    public Product(
        String name,
        String description,
        String category,
        String imageUrl,
        BigDecimal price,
        BigDecimal unitCost,
        Integer stock
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.price = price;
        this.unitCost = unitCost == null ? BigDecimal.ZERO : unitCost;
        this.stock = stock;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public String getImageUrl() { return imageUrl; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getUnitCost() { return unitCost; }
    public Integer getStock() { return stock; }
    public boolean isActive() { return active; }

    public void update(String name, String description, String category, BigDecimal price, Integer stock, boolean active) {
        update(name, description, category, imageUrl, price, unitCost, stock, active);
    }

    public void update(
        String name,
        String description,
        String category,
        String imageUrl,
        BigDecimal price,
        BigDecimal unitCost,
        Integer stock,
        boolean active
    ) {
        this.name = name;
        this.description = description;
        this.category = category;
        this.imageUrl = imageUrl;
        this.price = price;
        if (unitCost != null) this.unitCost = unitCost;
        this.stock = stock;
        this.active = active;
    }

    public void reduceStock(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        if (stock < quantity) throw new IllegalArgumentException("Inventario insuficiente para " + name);
        stock -= quantity;
    }

    public void restoreStock(int quantity) {
        if (quantity < 1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        stock += quantity;
    }
}

package com.gestourant.order;

import com.gestourant.catalog.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id")
    private RestaurantOrder order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "product_name_snapshot", nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "removed_ingredients", nullable = false, length = 500)
    private String removedIngredients = "";

    protected OrderItem() {}

    public OrderItem(RestaurantOrder order, Product product, int quantity) {
        this(order, product, quantity, product.getPrice());
    }

    public OrderItem(RestaurantOrder order, Product product, int quantity, BigDecimal unitPrice) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCost = product.getUnitCost();
        this.productName = product.getName();
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getUnitCost() { return unitCost; }
    public String getProductName() { return productName; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getRemovedIngredients() { return removedIngredients; }

    public void updateRemovedIngredients(String ingredients) {
        removedIngredients = ingredients;
    }
}

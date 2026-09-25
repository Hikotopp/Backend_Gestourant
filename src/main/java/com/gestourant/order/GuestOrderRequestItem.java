package com.gestourant.order;

import com.gestourant.catalog.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
@Table(name = "guest_order_request_items")
public class GuestOrderRequestItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "request_id")
    private GuestOrderRequest request;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "removed_ingredients", nullable = false, length = 500)
    private String removedIngredients = "";

    protected GuestOrderRequestItem() {}

    public GuestOrderRequestItem(GuestOrderRequest request, Product product, int quantity, String removedIngredients) {
        this.request = request;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = product.getPrice();
        this.removedIngredients = removedIngredients == null ? "" : removedIngredients.trim();
    }

    public BigDecimal getSubtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public String getRemovedIngredients() { return removedIngredients; }
}

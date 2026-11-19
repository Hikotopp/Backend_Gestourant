package com.gestourant.order;

import com.gestourant.catalog.Product;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;

@Entity
@Table(name = "product_return_items")
public class ProductReturnItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "return_id")
    private ProductReturn productReturn;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @Column(name = "product_name_snapshot", nullable = false, length = 120)
    private String productName;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundAmount;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(nullable = false)
    private boolean restocked;

    protected ProductReturnItem() {}

    public ProductReturnItem(
        ProductReturn productReturn,
        OrderItem orderItem,
        int quantity,
        boolean restocked
    ) {
        this.productReturn = productReturn;
        this.orderItem = orderItem;
        this.product = orderItem.getProduct();
        this.productName = orderItem.getProductName();
        this.quantity = quantity;
        this.unitPrice = orderItem.getUnitPrice();
        this.refundAmount = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.unitCost = orderItem.getUnitCost();
        this.restocked = restocked;
    }

    public Long getId() { return id; }
    public ProductReturn getProductReturn() { return productReturn; }
    public OrderItem getOrderItem() { return orderItem; }
    public Product getProduct() { return product; }
    public String getProductName() { return productName; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getRefundAmount() { return refundAmount; }
    public BigDecimal getUnitCost() { return unitCost; }
    public boolean isRestocked() { return restocked; }
    public BigDecimal getLossAmount() {
        return restocked ? BigDecimal.ZERO : unitCost.multiply(BigDecimal.valueOf(quantity));
    }
}

package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.time.ColombiaTime;
import com.gestourant.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory_losses")
public class InventoryLoss {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(optional = false)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @Column(name = "product_name_snapshot", nullable = false, length = 120)
    private String productName;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(nullable = false, length = 500)
    private String notes = "";

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "total_cost", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalCost;

    @Column(name = "reported_at", nullable = false)
    private LocalDateTime reportedAt = ColombiaTime.now();

    protected InventoryLoss() {}

    public InventoryLoss(Product product, User reportedBy, int quantity, String reason, String notes) {
        this.product = product;
        this.reportedBy = reportedBy;
        this.productName = product.getName();
        this.quantity = quantity;
        this.reason = reason;
        this.notes = notes;
        this.unitCost = product.getUnitCost();
        this.totalCost = unitCost.multiply(BigDecimal.valueOf(quantity));
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public User getReportedBy() { return reportedBy; }
    public String getProductName() { return productName; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitCost() { return unitCost; }
    public BigDecimal getTotalCost() { return totalCost; }
    public LocalDateTime getReportedAt() { return reportedAt; }
}

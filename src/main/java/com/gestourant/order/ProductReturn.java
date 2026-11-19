package com.gestourant.order;

import com.gestourant.time.ColombiaTime;
import com.gestourant.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "product_returns")
public class ProductReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "invoice_id")
    private Invoice invoice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(nullable = false, length = 40)
    private String reason;

    @Column(nullable = false, length = 500)
    private String notes = "";

    @Enumerated(EnumType.STRING)
    @Column(name = "refund_method", nullable = false, length = 20)
    private PaymentMethod refundMethod;

    @Column(name = "refund_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal refundTotal = BigDecimal.ZERO;

    @Column(name = "returned_at", nullable = false)
    private LocalDateTime returnedAt = ColombiaTime.now();

    @OneToMany(mappedBy = "productReturn", cascade = CascadeType.ALL)
    private List<ProductReturnItem> items = new ArrayList<>();

    protected ProductReturn() {}

    public ProductReturn(
        Invoice invoice,
        User processedBy,
        String reason,
        String notes,
        PaymentMethod refundMethod
    ) {
        this.invoice = invoice;
        this.processedBy = processedBy;
        this.reason = reason;
        this.notes = notes;
        this.refundMethod = refundMethod;
    }

    public void addItem(ProductReturnItem item) {
        items.add(item);
        refundTotal = refundTotal.add(item.getRefundAmount());
    }

    public Long getId() { return id; }
    public Invoice getInvoice() { return invoice; }
    public User getProcessedBy() { return processedBy; }
    public String getReason() { return reason; }
    public String getNotes() { return notes; }
    public PaymentMethod getRefundMethod() { return refundMethod; }
    public BigDecimal getRefundTotal() { return refundTotal; }
    public LocalDateTime getReturnedAt() { return returnedAt; }
    public List<ProductReturnItem> getItems() { return items; }
}

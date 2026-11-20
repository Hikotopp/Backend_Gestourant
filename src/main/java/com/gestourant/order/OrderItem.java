package com.gestourant.order;

import com.gestourant.catalog.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

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

    @Enumerated(EnumType.STRING)
    @Column(name = "kitchen_status", nullable = false, length = 20)
    private KitchenItemStatus kitchenStatus = KitchenItemStatus.BORRADOR;

    @Column(name = "kitchen_batch_id", length = 36)
    private String kitchenBatchId;

    @Column(name = "kitchen_submitted_at")
    private LocalDateTime kitchenSubmittedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "order_origin", nullable = false, length = 10)
    private OrderItemOrigin origin = OrderItemOrigin.STAFF;

    protected OrderItem() {}

    public OrderItem(RestaurantOrder order, Product product, int quantity) {
        this(order, product, quantity, product.getPrice());
    }

    public OrderItem(RestaurantOrder order, Product product, int quantity, BigDecimal unitPrice) {
        this(order, product, quantity, unitPrice, OrderItemOrigin.STAFF);
    }

    public OrderItem(
        RestaurantOrder order,
        Product product,
        int quantity,
        BigDecimal unitPrice,
        OrderItemOrigin origin
    ) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.unitCost = product.getUnitCost();
        this.productName = product.getName();
        this.subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
        this.origin = origin;
    }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public BigDecimal getUnitCost() { return unitCost; }
    public String getProductName() { return productName; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getRemovedIngredients() { return removedIngredients; }
    public KitchenItemStatus getKitchenStatus() { return kitchenStatus; }
    public String getKitchenBatchId() { return kitchenBatchId; }
    public LocalDateTime getKitchenSubmittedAt() { return kitchenSubmittedAt; }
    public OrderItemOrigin getOrigin() { return origin; }

    public void updateRemovedIngredients(String ingredients) {
        ensureDraft();
        removedIngredients = ingredients;
    }

    public void submitToKitchen(String batchId, LocalDateTime submittedAt) {
        ensureDraft();
        if (origin != OrderItemOrigin.STAFF) {
            throw new IllegalArgumentException("Las solicitudes QR siguen su propio flujo de cocina");
        }
        kitchenBatchId = batchId;
        kitchenSubmittedAt = submittedAt;
        kitchenStatus = KitchenItemStatus.ENVIADO;
    }

    public void advanceKitchenStatus() {
        kitchenStatus = switch (kitchenStatus) {
            case ENVIADO -> KitchenItemStatus.ACEPTADO;
            case ACEPTADO -> KitchenItemStatus.PREPARANDO;
            case PREPARANDO -> KitchenItemStatus.LISTO;
            default -> throw new IllegalArgumentException("El pedido no puede avanzar en este estado");
        };
    }

    public void markDelivered() {
        if (kitchenStatus != KitchenItemStatus.LISTO) {
            throw new IllegalArgumentException("Solo se pueden entregar pedidos listos");
        }
        kitchenStatus = KitchenItemStatus.ENTREGADO;
    }

    public void ensureDraft() {
        if (kitchenStatus != KitchenItemStatus.BORRADOR) {
            throw new IllegalArgumentException("El pedido ya fue enviado a cocina y no se puede modificar");
        }
    }
}

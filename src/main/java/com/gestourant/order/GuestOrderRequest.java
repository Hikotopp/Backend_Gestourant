package com.gestourant.order;

import com.gestourant.restaurant.RestaurantTable;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "guest_order_requests")
public class GuestOrderRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "table_id")
    private RestaurantTable table;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GuestRequestStatus status = GuestRequestStatus.PENDIENTE;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @Column(name = "guest_session_hash", length = 64)
    private String guestSessionHash;

    @OneToMany(mappedBy = "request", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GuestOrderRequestItem> items = new ArrayList<>();

    protected GuestOrderRequest() {}

    public GuestOrderRequest(RestaurantTable table) {
        this(table, null);
    }

    public GuestOrderRequest(RestaurantTable table, String guestSessionHash) {
        this.table = table;
        this.guestSessionHash = guestSessionHash;
    }

    public void addItem(GuestOrderRequestItem item) {
        items.add(item);
    }

    public void accept() {
        status = GuestRequestStatus.EN_COCINA;
        resolvedAt = LocalDateTime.now();
    }

    public void reject() {
        status = GuestRequestStatus.RECHAZADA;
        resolvedAt = LocalDateTime.now();
    }

    public void startPreparation() {
        if (status != GuestRequestStatus.EN_COCINA) {
            throw new IllegalArgumentException("Solo puedes iniciar un pedido que esté en cola de cocina");
        }
        status = GuestRequestStatus.PREPARANDO;
    }

    public void markReady() {
        if (status != GuestRequestStatus.PREPARANDO) {
            throw new IllegalArgumentException("El pedido debe estar en preparación antes de marcarlo listo");
        }
        status = GuestRequestStatus.LISTO;
    }

    public Long getId() { return id; }
    public RestaurantTable getTable() { return table; }
    public GuestRequestStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public String getGuestSessionHash() { return guestSessionHash; }
    public List<GuestOrderRequestItem> getItems() { return items; }
}

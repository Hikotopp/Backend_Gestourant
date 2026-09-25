package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.ProductRepository;
import com.gestourant.restaurant.RestaurantTable;
import com.gestourant.restaurant.RestaurantTableRepository;
import com.gestourant.restaurant.TableStatus;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class GuestOrderingService {
    private static final Pattern GUEST_SESSION_TOKEN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private final RestaurantTableRepository tables;
    private final ProductRepository products;
    private final GuestOrderRequestRepository requests;
    private final RestaurantOrderRepository orders;
    private final OrderService orderService;

    public GuestOrderingService(
        RestaurantTableRepository tables,
        ProductRepository products,
        GuestOrderRequestRepository requests,
        RestaurantOrderRepository orders,
        OrderService orderService
    ) {
        this.tables = tables;
        this.products = products;
        this.requests = requests;
        this.orders = orders;
        this.orderService = orderService;
    }

    public GuestTableView getTable(String token) {
        RestaurantTable table = getTableByToken(token);
        List<GuestProductView> menu = products.findByActiveTrueAndStockGreaterThanOrderByCategoryAscNameAsc(0)
            .stream()
            .map(product -> new GuestProductView(
                product.getId(), product.getName(), product.getDescription(), product.getCategory(),
                product.getImageUrl(), product.getPrice(), product.getStock()
            ))
            .toList();
        return new GuestTableView(table.getTableNumber(), menu);
    }

    public List<GuestRequestView> guestRequests(String tableToken, String guestSessionToken) {
        getTableByToken(tableToken);
        String sessionHash = hashGuestSessionToken(guestSessionToken);
        return requests.findTop15ByTableQrTokenAndGuestSessionHashOrderByCreatedAtDesc(tableToken, sessionHash)
            .stream()
            .map(this::toRequestView)
            .toList();
    }

    @Transactional
    public GuestRequestView createRequest(String token, String guestSessionToken, GuestOrderRequestPayload payload) {
        RestaurantTable table = getTableByToken(token);
        GuestOrderRequest request = new GuestOrderRequest(table, hashGuestSessionToken(guestSessionToken));
        for (GuestOrderItemPayload item : payload.items()) {
            Product product = products.findById(item.productId())
                .orElseThrow(() -> new IllegalArgumentException("Uno de los productos ya no está disponible"));
            if (!product.isActive() || product.getStock() < item.quantity()) {
                throw new IllegalArgumentException(product.getName() + " no está disponible en la cantidad solicitada");
            }
            request.addItem(new GuestOrderRequestItem(request, product, item.quantity(), item.removedIngredients()));
        }
        return toRequestView(requests.save(request));
    }

    @Transactional
    public void requestBill(String token) {
        RestaurantTable table = getTableByToken(token);
        table.requestBill();
        tables.save(table);
    }

    @Transactional
    public GuestRequestView approve(Long requestId) {
        GuestOrderRequest request = requests.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));
        ensurePending(request);
        RestaurantTable table = request.getTable();
        RestaurantOrder order = table.getStatus() == TableStatus.OCUPADA
            ? orders.findByTableIdAndStatus(table.getId(), OrderStatus.ABIERTA)
                .orElseThrow(() -> new IllegalStateException("La mesa figura ocupada pero no tiene una cuenta abierta"))
            : orderService.open(table.getId());

        for (GuestOrderRequestItem item : request.getItems()) {
            order = orderService.addItem(
                order.getId(), item.getProduct().getId(), item.getQuantity(),
                item.getUnitPrice(), item.getRemovedIngredients()
            );
        }
        request.accept();
        requests.save(request);
        return toRequestView(request);
    }

    @Transactional
    public GuestRequestView reject(Long requestId) {
        GuestOrderRequest request = requests.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("La solicitud no existe"));
        ensurePending(request);
        request.reject();
        return toRequestView(requests.save(request));
    }

    public List<GuestRequestView> pendingRequests() {
        return requests.findByStatusOrderByCreatedAtAsc(GuestRequestStatus.PENDIENTE)
            .stream()
            .map(this::toRequestView)
            .toList();
    }

    public List<GuestRequestView> kitchenRequests() {
        return requests.findByStatusInOrderByCreatedAtAsc(
            List.of(GuestRequestStatus.EN_COCINA, GuestRequestStatus.PREPARANDO)
        ).stream().map(this::toRequestView).toList();
    }

    @Transactional
    public GuestRequestView advanceKitchenRequest(Long requestId) {
        GuestOrderRequest request = requests.findById(requestId)
            .orElseThrow(() -> new IllegalArgumentException("El pedido no existe"));
        if (request.getStatus() == GuestRequestStatus.EN_COCINA) {
            request.startPreparation();
        } else if (request.getStatus() == GuestRequestStatus.PREPARANDO) {
            request.markReady();
        } else {
            throw new IllegalArgumentException("Este pedido ya salió de la cola de cocina");
        }
        return toRequestView(requests.save(request));
    }

    private RestaurantTable getTableByToken(String token) {
        if (token == null || token.length() != 36) {
            throw new IllegalArgumentException("El código QR no es válido");
        }
        return tables.findByQrToken(token)
            .orElseThrow(() -> new IllegalArgumentException("No encontramos esta mesa. Pide ayuda a un empleado."));
    }

    private String hashGuestSessionToken(String guestSessionToken) {
        if (guestSessionToken == null || !GUEST_SESSION_TOKEN.matcher(guestSessionToken).matches()) {
            throw new IllegalArgumentException("No se pudo validar esta sesión del cliente. Vuelve a abrir el QR.");
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(guestSessionToken.getBytes(StandardCharsets.UTF_8));
            StringBuilder hash = new StringBuilder(digest.length * 2);
            for (byte value : digest) {
                hash.append(Character.forDigit((value >>> 4) & 0xf, 16));
                hash.append(Character.forDigit(value & 0xf, 16));
            }
            return hash.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("No se pudo proteger la sesión del cliente.", exception);
        }
    }

    private void ensurePending(GuestOrderRequest request) {
        if (request.getStatus() != GuestRequestStatus.PENDIENTE) {
            throw new IllegalArgumentException("Esta solicitud ya fue atendida");
        }
    }

    private GuestRequestView toRequestView(GuestOrderRequest request) {
        List<GuestRequestItemView> items = request.getItems().stream()
            .map(item -> new GuestRequestItemView(
                item.getProduct().getName(), item.getQuantity(), item.getUnitPrice(),
                item.getSubtotal(), item.getRemovedIngredients()
            ))
            .toList();
        BigDecimal total = items.stream().map(GuestRequestItemView::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GuestRequestView(
            request.getId(), request.getTable().getTableNumber(), request.getStatus(),
            request.getCreatedAt(), items, total
        );
    }

    public record GuestOrderRequestPayload(
        @jakarta.validation.constraints.NotEmpty
        @jakarta.validation.constraints.Size(max = 20)
        List<@jakarta.validation.Valid GuestOrderItemPayload> items
    ) {}

    public record GuestOrderItemPayload(
        @jakarta.validation.constraints.NotNull Long productId,
        @jakarta.validation.constraints.NotNull @jakarta.validation.constraints.Min(1)
        @jakarta.validation.constraints.Max(10) Integer quantity,
        @jakarta.validation.constraints.Size(max = 500) String removedIngredients
    ) {}

    public record GuestProductView(Long id, String name, String description, String category, String imageUrl, BigDecimal price, Integer stock) {}
    public record GuestRequestItemView(String name, Integer quantity, BigDecimal unitPrice, BigDecimal subtotal, String removedIngredients) {}
    public record GuestRequestView(Long id, Integer tableNumber, GuestRequestStatus status, java.time.LocalDateTime createdAt, List<GuestRequestItemView> items, BigDecimal total) {}
    public record GuestTableView(Integer tableNumber, List<GuestProductView> menu) {}
}

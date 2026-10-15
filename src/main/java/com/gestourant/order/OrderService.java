package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import com.gestourant.restaurant.RestaurantTable;
import com.gestourant.restaurant.RestaurantTableRepository;
import com.gestourant.restaurant.TableStatus;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderService {
    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final RestaurantTableRepository tables;
    private final RestaurantOrderRepository orders;
    private final ProductCatalogPort products;
    private final InvoiceRepository invoices;
    private final UserRepository users;
    private final GuestOrderRequestRepository guestRequests;

    public OrderService(
        RestaurantTableRepository tables,
        RestaurantOrderRepository orders,
        ProductCatalogPort products,
        InvoiceRepository invoices,
        UserRepository users,
        GuestOrderRequestRepository guestRequests
    ) {
        this.tables = tables;
        this.orders = orders;
        this.products = products;
        this.invoices = invoices;
        this.users = users;
        this.guestRequests = guestRequests;
    }

    @Transactional
    public RestaurantOrder open(Long tableId) {
        RestaurantTable table = tables.findById(tableId)
            .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada"));
        if (table.getStatus() == TableStatus.OCUPADA) {
            throw new IllegalArgumentException("La mesa ya está ocupada");
        }

        User user = currentUser();
        table.occupy();
        RestaurantOrder order = orders.save(new RestaurantOrder(table, user));
        log.info("Order opened orderId={} tableId={} user={}", order.getId(), tableId, user.getUsername());
        return order;
    }

    @Transactional
    public RestaurantOrder addItem(Long orderId, Long productId, int quantity) {
        return addItem(orderId, productId, quantity, null, "");
    }

    @Transactional
    public RestaurantOrder addItem(Long orderId, Long productId, int quantity, BigDecimal agreedPrice) {
        return addItem(orderId, productId, quantity, agreedPrice, "");
    }

    @Transactional
    public RestaurantOrder addItem(
        Long orderId,
        Long productId,
        int quantity,
        BigDecimal agreedPrice,
        String removedIngredients
    ) {
        RestaurantOrder order = getOpen(orderId);
        Product product = products.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (!product.isActive()) {
            throw new IllegalArgumentException("El producto no está disponible");
        }

        product.reduceStock(quantity);
        OrderItem item = new OrderItem(
            order,
            product,
            quantity,
            agreedPrice == null ? product.getPrice() : agreedPrice
        );
        item.updateRemovedIngredients(removedIngredients == null ? "" : removedIngredients);
        order.addItem(item);
        return orders.save(order);
    }

    @Transactional
    public RestaurantOrder updateItemIngredients(Long orderId, Long itemId, String removedIngredients) {
        RestaurantOrder order = getOpen(orderId);
        OrderItem item = order.getItems().stream()
            .filter(candidate -> candidate.getId().equals(itemId))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El producto no pertenece a este pedido"));

        item.updateRemovedIngredients(removedIngredients == null ? "" : removedIngredients.trim());
        return orders.save(order);
    }

    @Transactional
    public RestaurantOrder removeItem(Long orderId, Long itemId) {
        RestaurantOrder order = getOpen(orderId);
        OrderItem item = order.removeItem(itemId);
        item.getProduct().restoreStock(item.getQuantity());
        return orders.save(order);
    }

    @Transactional
    public Invoice close(Long orderId, PaymentMethod method) {
        RestaurantOrder order = getOpen(orderId);
        Long tableId = order.getTable().getId();
        if (guestRequests.existsByTableIdAndStatus(tableId, GuestRequestStatus.PENDIENTE)) {
            throw new IllegalArgumentException("Revisa o rechaza las solicitudes QR pendientes antes de cerrar la cuenta");
        }
        if (guestRequests.existsByTableIdAndStatusIn(
            tableId,
            List.of(GuestRequestStatus.EN_COCINA, GuestRequestStatus.PREPARANDO)
        )) {
            throw new IllegalArgumentException("Espera a que cocina marque todos los pedidos como listos antes de cerrar la cuenta");
        }
        if (order.getItems().isEmpty()) {
            throw new IllegalArgumentException("El pedido no tiene productos");
        }

        order.close();
        order.getTable().free();
        order.getTable().clearBillRequest();
        Invoice invoice = invoices.save(new Invoice(order, method));
        log.info(
            "Order closed orderId={} invoice={} total={} payment={}",
            orderId,
            invoice.getInvoiceNumber(),
            invoice.getTotal(),
            method
        );
        return invoice;
    }

    public RestaurantOrder get(Long id) {
        return orders.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
    }

    public RestaurantOrder getOpenByTable(Long tableId) {
        return orders.findByTableIdAndStatus(tableId, OrderStatus.ABIERTA)
            .orElseThrow(() -> new IllegalArgumentException("No hay un pedido abierto para esta mesa"));
    }

    public CashClose cashClose(LocalDate date) {
        User user = currentUser();
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean administrator = authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));

        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.plusDays(1).atStartOfDay();
        List<Invoice> invoicesForDate = administrator
            ? invoices.findByIssuedAtBetween(start, end)
            : invoices.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
                start,
                end,
                user.getId()
            );

        BigDecimal cash = sum(invoicesForDate, PaymentMethod.EFECTIVO);
        BigDecimal digital = sum(invoicesForDate, PaymentMethod.DIGITAL);
        return new CashClose(date, cash, digital, cash.add(digital), invoicesForDate.size());
    }

    public InvoiceHistory employeeInvoiceHistory(LocalDate from, LocalDate to) {
        return invoiceHistory(from, to, currentUser().getId());
    }

    public InvoiceHistory allInvoiceHistory(LocalDate from, LocalDate to) {
        return invoiceHistory(from, to, null);
    }

    private RestaurantOrder getOpen(Long id) {
        RestaurantOrder order = get(id);
        if (order.getStatus() != OrderStatus.ABIERTA) {
            throw new IllegalArgumentException("El pedido ya fue cerrado");
        }
        return order;
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    private InvoiceHistory invoiceHistory(LocalDate from, LocalDate to, Long employeeId) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Selecciona una fecha inicial y final");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la fecha inicial");
        }

        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<Invoice> invoiceList = employeeId == null
            ? invoices.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanOrderByIssuedAtAsc(start, end)
            : invoices.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
                start,
                end,
                employeeId
            );

        BigDecimal cash = sum(invoiceList, PaymentMethod.EFECTIVO);
        BigDecimal digital = sum(invoiceList, PaymentMethod.DIGITAL);
        List<InvoiceDetail> details = invoiceList.stream()
            .map(invoice -> new InvoiceDetail(
                invoice.getInvoiceNumber(),
                invoice.getIssuedAt(),
                invoice.getOrder().getTable().getTableNumber(),
                invoice.getPaymentMethod().name(),
                invoice.getTotal(),
                invoice.getOrder().createdByForReport().getUsername()
            ))
            .toList();

        return new InvoiceHistory(from, to, cash, digital, cash.add(digital), details.size(), details);
    }

    private BigDecimal sum(List<Invoice> invoiceList, PaymentMethod method) {
        return invoiceList.stream()
            .filter(invoice -> invoice.getPaymentMethod() == method)
            .map(Invoice::getTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record CashClose(
        LocalDate date,
        BigDecimal efectivo,
        BigDecimal digital,
        BigDecimal total,
        long facturas
    ) {}

    public record InvoiceDetail(
        String invoiceNumber,
        LocalDateTime issuedAt,
        Integer tableNumber,
        String paymentMethod,
        BigDecimal total,
        String employee
    ) {}

    public record InvoiceHistory(
        LocalDate from,
        LocalDate to,
        BigDecimal efectivo,
        BigDecimal digital,
        BigDecimal total,
        long facturas,
        List<InvoiceDetail> invoices
    ) {}
}

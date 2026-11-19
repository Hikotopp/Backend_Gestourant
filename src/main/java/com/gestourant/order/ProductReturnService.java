package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductReturnService {
    private final InvoiceRepository invoices;
    private final RestaurantOrderRepository orders;
    private final ProductReturnRepository returns;
    private final UserRepository users;

    public ProductReturnService(
        InvoiceRepository invoices,
        RestaurantOrderRepository orders,
        ProductReturnRepository returns,
        UserRepository users
    ) {
        this.invoices = invoices;
        this.orders = orders;
        this.returns = returns;
        this.users = users;
    }

    @Transactional
    public ReturnHistory history(Long invoiceId) {
        Invoice invoice = invoices.findById(invoiceId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        RestaurantOrder order = orders.findById(invoice.getOrder().getId())
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        assertCanAccess(order);

        List<ProductReturn> invoiceReturns = returns.findByInvoiceIdOrderByReturnedAtDesc(invoiceId);
        Map<Long, Integer> returnedQuantities = new LinkedHashMap<>();
        List<ReturnDetail> details = invoiceReturns.stream()
            .map(productReturn -> {
                ProductReturnItem item = productReturn.getItems().get(0);
                returnedQuantities.merge(item.getOrderItem().getId(), item.getQuantity(), Integer::sum);
                return ReturnDetail.from(productReturn, item);
            })
            .toList();
        return new ReturnHistory(invoiceId, returnedQuantities, details);
    }

    @Transactional
    public ReturnDetail create(Long invoiceId, ReturnCommand command) {
        Invoice invoice = invoices.findForUpdateById(invoiceId)
            .orElseThrow(() -> new IllegalArgumentException("Factura no encontrada"));
        RestaurantOrder order = orders.findById(invoice.getOrder().getId())
            .orElseThrow(() -> new IllegalArgumentException("Pedido no encontrado"));
        assertCanAccess(order);
        if (order.getStatus() != OrderStatus.PAGADA) {
            throw new IllegalArgumentException("Solo se pueden registrar devoluciones de cuentas pagadas");
        }

        OrderItem orderItem = order.getItems().stream()
            .filter(item -> item.getId().equals(command.orderItemId()))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("El producto no pertenece a esta factura"));

        int alreadyReturned = returns.findByInvoiceIdOrderByReturnedAtDesc(invoiceId).stream()
            .flatMap(productReturn -> productReturn.getItems().stream())
            .filter(item -> item.getOrderItem().getId().equals(orderItem.getId()))
            .mapToInt(ProductReturnItem::getQuantity)
            .sum();
        int remaining = orderItem.getQuantity() - alreadyReturned;
        if (command.quantity() > remaining) {
            throw new IllegalArgumentException("La cantidad supera las unidades aún disponibles para devolución");
        }

        User user = currentUser();
        String notes = command.notes() == null ? "" : command.notes().trim();
        ProductReturn productReturn = new ProductReturn(
            invoice,
            user,
            command.reason(),
            notes,
            command.refundMethod()
        );
        ProductReturnItem returnItem = new ProductReturnItem(
            productReturn,
            orderItem,
            command.quantity(),
            command.restocked()
        );
        productReturn.addItem(returnItem);
        if (command.restocked()) {
            Product product = orderItem.getProduct();
            product.restoreStock(command.quantity());
        }

        return ReturnDetail.from(returns.save(productReturn), returnItem);
    }

    private void assertCanAccess(RestaurantOrder order) {
        User user = currentUser();
        if (user.getRole() != Role.ADMINISTRADOR
            && !user.getId().equals(order.createdByForReport().getId())) {
            throw new AccessDeniedException("No puedes consultar ni modificar la factura de otro empleado");
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    public record ReturnCommand(
        Long orderItemId,
        int quantity,
        String reason,
        String notes,
        boolean restocked,
        PaymentMethod refundMethod
    ) {}

    public record ReturnHistory(
        Long invoiceId,
        Map<Long, Integer> returnedQuantities,
        List<ReturnDetail> returns
    ) {}

    public record ReturnDetail(
        Long id,
        Long orderItemId,
        String productName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal refundAmount,
        boolean restocked,
        BigDecimal lossAmount,
        String reason,
        String notes,
        String refundMethod,
        String processedBy,
        java.time.LocalDateTime returnedAt
    ) {
        static ReturnDetail from(ProductReturn productReturn, ProductReturnItem item) {
            return new ReturnDetail(
                productReturn.getId(),
                item.getOrderItem().getId(),
                item.getProductName(),
                item.getQuantity(),
                item.getUnitPrice(),
                item.getRefundAmount(),
                item.isRestocked(),
                item.getLossAmount(),
                productReturn.getReason(),
                productReturn.getNotes(),
                productReturn.getRefundMethod().name(),
                productReturn.getProcessedBy().getUsername(),
                productReturn.getReturnedAt()
            );
        }
    }
}

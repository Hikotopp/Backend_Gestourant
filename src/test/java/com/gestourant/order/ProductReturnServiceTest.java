package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductReturnServiceTest {
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
    private final ProductReturnRepository returns = mock(ProductReturnRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final ProductReturnService service = new ProductReturnService(invoices, orders, returns, users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsRefundWithoutRestockingAndSnapshotsTheLoss() {
        Fixture fixture = fixture();
        when(returns.findByInvoiceIdOrderByReturnedAtDesc(8L)).thenReturn(List.of());
        when(returns.save(org.mockito.ArgumentMatchers.any(ProductReturn.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        ProductReturnService.ReturnDetail result = service.create(
            8L,
            new ProductReturnService.ReturnCommand(11L, 1, "CALIDAD", "Objeto extraño", false, PaymentMethod.DIGITAL)
        );

        assertEquals(new BigDecimal("24000.00"), result.refundAmount());
        assertEquals(new BigDecimal("7000.00"), result.lossAmount());
        assertEquals("Sopa del día", result.productName());
        verify(fixture.product, never()).restoreStock(1);
        verify(returns).save(org.mockito.ArgumentMatchers.any(ProductReturn.class));
    }

    @Test
    void restocksReturnedItemsAndRejectsReturningMoreThanWasSold() {
        Fixture fixture = fixture();
        when(returns.findByInvoiceIdOrderByReturnedAtDesc(8L)).thenReturn(List.of());
        when(returns.save(org.mockito.ArgumentMatchers.any(ProductReturn.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        service.create(
            8L,
            new ProductReturnService.ReturnCommand(11L, 1, "ERROR_PEDIDO", "", true, PaymentMethod.EFECTIVO)
        );

        verify(fixture.product).restoreStock(1);
        ProductReturn alreadyReturned = previousReturn();
        when(returns.findByInvoiceIdOrderByReturnedAtDesc(8L)).thenReturn(List.of(alreadyReturned));
        assertThrows(IllegalArgumentException.class, () -> service.create(
            8L,
            new ProductReturnService.ReturnCommand(11L, 1, "ERROR_PEDIDO", "", false, PaymentMethod.EFECTIVO)
        ));
    }

    private Fixture fixture() {
        User employee = mock(User.class);
        User owner = mock(User.class);
        when(employee.getRole()).thenReturn(Role.EMPLEADO);
        when(employee.getId()).thenReturn(3L);
        when(employee.getUsername()).thenReturn("cajero");
        when(owner.getId()).thenReturn(3L);
        when(users.findByEmailIgnoreCase("cashier@example.com")).thenReturn(Optional.of(employee));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cashier@example.com", "token")
        );

        Product product = mock(Product.class);
        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(11L);
        when(orderItem.getQuantity()).thenReturn(1);
        when(orderItem.getUnitPrice()).thenReturn(new BigDecimal("24000.00"));
        when(orderItem.getUnitCost()).thenReturn(new BigDecimal("7000.00"));
        when(orderItem.getProductName()).thenReturn("Sopa del día");
        when(orderItem.getProduct()).thenReturn(product);

        RestaurantOrder order = mock(RestaurantOrder.class);
        when(order.getId()).thenReturn(100L);
        when(order.getStatus()).thenReturn(OrderStatus.PAGADA);
        when(order.getItems()).thenReturn(List.of(orderItem));
        when(order.createdByForReport()).thenReturn(owner);
        Invoice invoice = mock(Invoice.class);
        when(invoice.getOrder()).thenReturn(order);
        when(invoices.findForUpdateById(8L)).thenReturn(Optional.of(invoice));
        when(orders.findById(100L)).thenReturn(Optional.of(order));
        return new Fixture(product);
    }

    private ProductReturn previousReturn() {
        ProductReturn productReturn = mock(ProductReturn.class);
        ProductReturnItem item = mock(ProductReturnItem.class);
        OrderItem orderItem = mock(OrderItem.class);
        when(orderItem.getId()).thenReturn(11L);
        when(item.getOrderItem()).thenReturn(orderItem);
        when(item.getQuantity()).thenReturn(1);
        when(productReturn.getItems()).thenReturn(List.of(item));
        return productReturn;
    }

    private record Fixture(Product product) {}
}

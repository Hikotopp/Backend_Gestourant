package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import com.gestourant.restaurant.RestaurantTable;
import com.gestourant.restaurant.RestaurantTableRepository;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderPaymentWorkflowTest {
    @Test
    void blocksPaymentUntilEveryStaffOrderItemIsDelivered() {
        RestaurantTable table = new RestaurantTable(4, 2);
        User user = mock(User.class);
        RestaurantOrder order = new RestaurantOrder(table, user);
        ReflectionTestUtils.setField(order, "id", 42L);
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(new BigDecimal("10000"));
        when(product.getUnitCost()).thenReturn(new BigDecimal("4000"));
        when(product.getName()).thenReturn("Sopa");
        order.addItem(new OrderItem(order, product, 1));

        RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        ProductCatalogPort products = mock(ProductCatalogPort.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        UserRepository users = mock(UserRepository.class);
        GuestOrderRequestRepository guestRequests = mock(GuestOrderRequestRepository.class);
        when(orders.findById(42L)).thenReturn(Optional.of(order));
        OrderService service = new OrderService(tables, orders, products, invoices, users, guestRequests);

        assertThrows(IllegalArgumentException.class, () -> service.close(42L, PaymentMethod.EFECTIVO));

        assertEquals(OrderStatus.ABIERTA, order.getStatus());
        verifyNoInteractions(invoices);
    }

    @Test
    void keepsQrRequestOrdersOnTheirExistingKitchenWorkflow() {
        RestaurantTable table = new RestaurantTable(4, 2);
        RestaurantOrder order = new RestaurantOrder(table, mock(User.class));
        ReflectionTestUtils.setField(order, "id", 43L);
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(new BigDecimal("10000"));
        when(product.getUnitCost()).thenReturn(new BigDecimal("4000"));
        when(product.getName()).thenReturn("Sopa");
        order.addItem(new OrderItem(order, product, 1, new BigDecimal("10000"), OrderItemOrigin.QR));

        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        InvoiceRepository invoices = mock(InvoiceRepository.class);
        when(orders.findById(43L)).thenReturn(Optional.of(order));
        when(invoices.save(any(Invoice.class))).thenReturn(mock(Invoice.class));
        OrderService service = new OrderService(
            mock(RestaurantTableRepository.class),
            orders,
            mock(ProductCatalogPort.class),
            invoices,
            mock(UserRepository.class),
            mock(GuestOrderRequestRepository.class)
        );

        service.close(43L, PaymentMethod.EFECTIVO);

        assertEquals(OrderStatus.PAGADA, order.getStatus());
    }
}

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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderServiceKitchenFlowTest {
    @Test
    void submitsAcceptsPreparesAndDeliversAStaffOrderBatch() {
        RestaurantTable table = new RestaurantTable(7, 4);
        RestaurantOrder order = new RestaurantOrder(table, mock(User.class));
        ReflectionTestUtils.setField(order, "id", 42L);
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(new BigDecimal("12000"));
        when(product.getUnitCost()).thenReturn(new BigDecimal("5000"));
        when(product.getName()).thenReturn("Arroz");
        OrderItem item = new OrderItem(order, product, 2);
        order.addItem(item);

        RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
        when(orders.findById(42L)).thenReturn(Optional.of(order));
        when(orders.findByStatusOrderByOpenedAtAsc(OrderStatus.ABIERTA)).thenReturn(List.of(order));
        when(orders.save(any(RestaurantOrder.class))).thenAnswer(invocation -> invocation.getArgument(0));
        OrderService service = new OrderService(
            mock(RestaurantTableRepository.class),
            orders,
            mock(ProductCatalogPort.class),
            mock(InvoiceRepository.class),
            mock(UserRepository.class),
            mock(GuestOrderRequestRepository.class)
        );

        service.submitToKitchen(42L);
        String batchId = item.getKitchenBatchId();
        assertEquals(KitchenItemStatus.ENVIADO, item.getKitchenStatus());
        assertEquals(KitchenItemStatus.ENVIADO, service.kitchenOrderTickets().get(0).status());
        assertThrows(IllegalArgumentException.class, () -> service.deliverKitchenBatch(42L, batchId));

        when(orders.findKitchenBatchOrder(OrderStatus.ABIERTA, batchId)).thenReturn(Optional.of(order));
        service.advanceKitchenBatch(batchId);
        assertEquals(KitchenItemStatus.ACEPTADO, item.getKitchenStatus());
        service.advanceKitchenBatch(batchId);
        assertEquals(KitchenItemStatus.PREPARANDO, item.getKitchenStatus());
        service.advanceKitchenBatch(batchId);
        assertEquals(KitchenItemStatus.LISTO, item.getKitchenStatus());

        service.deliverKitchenBatch(42L, batchId);

        assertEquals(KitchenItemStatus.ENTREGADO, item.getKitchenStatus());
        assertEquals(List.of(), service.kitchenOrderTickets());
    }
}

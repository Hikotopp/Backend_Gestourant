package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.restaurant.RestaurantTable;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OrderItemKitchenWorkflowTest {
    @Test
    void advancesFromDraftThroughDeliveryInOrder() {
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(new BigDecimal("10000"));
        when(product.getUnitCost()).thenReturn(new BigDecimal("4000"));
        when(product.getName()).thenReturn("Sopa");
        RestaurantOrder order = new RestaurantOrder(new RestaurantTable(1, 4), null);
        OrderItem item = new OrderItem(order, product, 1);

        assertThrows(IllegalArgumentException.class, item::advanceKitchenStatus);
        item.submitToKitchen("batch-1", LocalDateTime.now());
        assertEquals(KitchenItemStatus.ENVIADO, item.getKitchenStatus());
        item.advanceKitchenStatus();
        assertEquals(KitchenItemStatus.ACEPTADO, item.getKitchenStatus());
        item.advanceKitchenStatus();
        assertEquals(KitchenItemStatus.PREPARANDO, item.getKitchenStatus());
        item.advanceKitchenStatus();
        assertEquals(KitchenItemStatus.LISTO, item.getKitchenStatus());
        item.markDelivered();
        assertEquals(KitchenItemStatus.ENTREGADO, item.getKitchenStatus());
    }

    @Test
    void preventsEditingItemsAfterSubmission() {
        Product product = mock(Product.class);
        when(product.getPrice()).thenReturn(new BigDecimal("10000"));
        when(product.getUnitCost()).thenReturn(new BigDecimal("4000"));
        when(product.getName()).thenReturn("Sopa");
        OrderItem item = new OrderItem(new RestaurantOrder(new RestaurantTable(1, 4), null), product, 1);
        item.submitToKitchen("batch-2", LocalDateTime.now());

        assertThrows(IllegalArgumentException.class, () -> item.updateRemovedIngredients("sin cilantro"));
    }
}

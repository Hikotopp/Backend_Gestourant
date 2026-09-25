package com.gestourant.order;

import com.gestourant.catalog.ProductRepository;
import com.gestourant.restaurant.RestaurantTable;
import com.gestourant.restaurant.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestOrderingServiceTest {
    private static final String TABLE_TOKEN = "123e4567-e89b-12d3-a456-426614174000";
    private final RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final GuestOrderRequestRepository requests = mock(GuestOrderRequestRepository.class);
    private final RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
    private final OrderService orderService = mock(OrderService.class);
    private final GuestOrderingService service = new GuestOrderingService(tables, products, requests, orders, orderService);

    @BeforeEach
    void setUp() {
        when(tables.findByQrToken(TABLE_TOKEN)).thenReturn(Optional.of(new RestaurantTable(1, 4)));
    }

    @Test
    void scopesCustomerRequestHistoryToHashedBrowserSession() throws Exception {
        String sessionToken = "a".repeat(64);
        String expectedHash = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(sessionToken.getBytes(StandardCharsets.UTF_8))
        );
        when(requests.findTop15ByTableQrTokenAndGuestSessionHashOrderByCreatedAtDesc(TABLE_TOKEN, expectedHash))
            .thenReturn(List.of());

        assertEquals(List.of(), service.guestRequests(TABLE_TOKEN, sessionToken));

        verify(requests).findTop15ByTableQrTokenAndGuestSessionHashOrderByCreatedAtDesc(TABLE_TOKEN, expectedHash);
    }

    @Test
    void rejectsMalformedBrowserSessionTokens() {
        assertThrows(IllegalArgumentException.class, () -> service.guestRequests(TABLE_TOKEN, "invalid"));

        verifyNoInteractions(requests);
    }

    @Test
    void publicMenuViewContainsNoSharedAccountOrTableOrderHistory() {
        when(products.findByActiveTrueAndStockGreaterThanOrderByCategoryAscNameAsc(0)).thenReturn(List.of());

        GuestOrderingService.GuestTableView view = service.getTable(TABLE_TOKEN);

        assertEquals(1, view.tableNumber());
        assertEquals(List.of(), view.menu());
        assertEquals(List.of("tableNumber", "menu"),
            java.util.Arrays.stream(view.getClass().getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList());
    }
}

package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import com.gestourant.restaurant.RestaurantTable;
import com.gestourant.restaurant.RestaurantTableRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentCaptor.forClass;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class GuestOrderingServiceTest {
    private static final String TABLE_TOKEN = "123e4567-e89b-12d3-a456-426614174000";
    private final RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
    private final ProductCatalogPort products = mock(ProductCatalogPort.class);
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
        when(products.findAvailable()).thenReturn(List.of());

        GuestOrderingService.GuestTableView view = service.getTable(TABLE_TOKEN);

        assertEquals(1, view.tableNumber());
        assertEquals(List.of(), view.menu());
        assertEquals(List.of("tableNumber", "menu"),
            java.util.Arrays.stream(view.getClass().getRecordComponents())
                .map(java.lang.reflect.RecordComponent::getName)
                .toList());
    }

    @Test
    void createsGuestRequestWithHashedSessionAndProductInstructions() throws Exception {
        Product product = new Product("Limonada", "Natural", "BEBIDA", new BigDecimal("7000"), 10);
        ReflectionTestUtils.setField(product, "id", 4L);
        when(products.findById(4L)).thenReturn(Optional.of(product));
        when(requests.save(any(GuestOrderRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        String sessionToken = "b".repeat(64);
        String expectedHash = HexFormat.of().formatHex(
            MessageDigest.getInstance("SHA-256").digest(sessionToken.getBytes(StandardCharsets.UTF_8))
        );

        GuestOrderingService.GuestRequestView view = service.createRequest(
            TABLE_TOKEN,
            sessionToken,
            new GuestOrderingService.GuestOrderRequestPayload(List.of(
                new GuestOrderingService.GuestOrderItemPayload(4L, 2, "sin hielo")
            ))
        );

        assertEquals(GuestRequestStatus.PENDIENTE, view.status());
        assertEquals(new BigDecimal("14000"), view.total());
        assertEquals("Limonada", view.items().get(0).name());
        assertEquals("sin hielo", view.items().get(0).removedIngredients());
        var savedRequest = forClass(GuestOrderRequest.class);
        verify(requests).save(savedRequest.capture());
        assertEquals(expectedHash, savedRequest.getValue().getGuestSessionHash());
    }

    @Test
    void approvalOpensTableOrderAndPreservesGuestPriceAndInstructionsForKitchen() {
        RestaurantTable table = new RestaurantTable(1, 4);
        ReflectionTestUtils.setField(table, "id", 12L);
        Product product = new Product("Limonada", "Natural", "BEBIDA", new BigDecimal("7000"), 10);
        ReflectionTestUtils.setField(product, "id", 4L);
        GuestOrderRequest request = new GuestOrderRequest(table, "hashed-session");
        request.addItem(new GuestOrderRequestItem(request, product, 2, "sin hielo"));
        ReflectionTestUtils.setField(request, "id", 55L);
        RestaurantOrder order = mock(RestaurantOrder.class);
        when(order.getId()).thenReturn(44L);
        when(requests.findById(55L)).thenReturn(Optional.of(request));
        when(orderService.open(12L)).thenReturn(order);
        when(requests.save(any(GuestOrderRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        GuestOrderingService.GuestRequestView view = service.approve(55L);

        assertEquals(GuestRequestStatus.EN_COCINA, view.status());
        verify(orderService).open(12L);
        verify(orderService).addItem(44L, 4L, 2, new BigDecimal("7000"), "sin hielo");
        verify(requests).save(request);
    }
}

package com.gestourant.order;

import com.gestourant.restaurant.RestaurantTable;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GuestOrderRequestTest {

    @Test
    void movesFromCustomerApprovalThroughKitchenStates() {
        GuestOrderRequest request = new GuestOrderRequest(new RestaurantTable(1, 4));

        request.accept();
        assertEquals(GuestRequestStatus.EN_COCINA, request.getStatus());

        request.startPreparation();
        assertEquals(GuestRequestStatus.PREPARANDO, request.getStatus());

        request.markReady();
        assertEquals(GuestRequestStatus.LISTO, request.getStatus());
    }

    @Test
    void cannotMarkNewKitchenRequestReadyBeforePreparation() {
        GuestOrderRequest request = new GuestOrderRequest(new RestaurantTable(1, 4));

        assertThrows(IllegalArgumentException.class, request::markReady);
    }

    @Test
    void storesTheGuestSessionHashForPrivateRequestLookup() {
        GuestOrderRequest request = new GuestOrderRequest(new RestaurantTable(1, 4), "hashed-session");

        assertEquals("hashed-session", request.getGuestSessionHash());
    }
}

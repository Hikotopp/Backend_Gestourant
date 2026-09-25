package com.gestourant.order;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class GuestOrderingController {
    private final GuestOrderingService service;

    public GuestOrderingController(GuestOrderingService service) {
        this.service = service;
    }

    @GetMapping("/api/guest/{token}")
    public GuestOrderingService.GuestTableView table(@PathVariable String token) {
        return service.getTable(token);
    }

    @GetMapping("/api/guest/{token}/requests")
    public List<GuestOrderingService.GuestRequestView> guestRequests(
        @PathVariable String token,
        @RequestHeader("X-Guest-Session") String guestSessionToken
    ) {
        return service.guestRequests(token, guestSessionToken);
    }

    @PostMapping("/api/guest/{token}/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public GuestOrderingService.GuestRequestView request(
        @PathVariable String token,
        @RequestHeader("X-Guest-Session") String guestSessionToken,
        @Valid @RequestBody GuestOrderingService.GuestOrderRequestPayload payload
    ) {
        return service.createRequest(token, guestSessionToken, payload);
    }

    @PostMapping("/api/guest/{token}/bill-request")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void requestBill(@PathVariable String token) {
        service.requestBill(token);
    }

    @GetMapping("/api/guest-requests")
    public List<GuestOrderingService.GuestRequestView> pendingRequests() {
        return service.pendingRequests();
    }

    @GetMapping("/api/kitchen/requests")
    public List<GuestOrderingService.GuestRequestView> kitchenRequests() {
        return service.kitchenRequests();
    }

    @PatchMapping("/api/kitchen/requests/{id}/advance")
    public GuestOrderingService.GuestRequestView advanceKitchenRequest(@PathVariable Long id) {
        return service.advanceKitchenRequest(id);
    }

    @PostMapping("/api/guest-requests/{id}/approve")
    public GuestOrderingService.GuestRequestView approve(@PathVariable Long id) {
        return service.approve(id);
    }

    @PostMapping("/api/guest-requests/{id}/reject")
    public GuestOrderingService.GuestRequestView reject(@PathVariable Long id) {
        return service.reject(id);
    }
}

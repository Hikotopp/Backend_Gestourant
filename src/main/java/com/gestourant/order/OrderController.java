package com.gestourant.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api")
public class OrderController {
    private final OrderService service;

    public OrderController(OrderService service) {
        this.service = service;
    }

    @PostMapping("/tables/{tableId}/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantOrder open(@PathVariable Long tableId) {
        return service.open(tableId);
    }

    @GetMapping("/tables/{tableId}/orders/open")
    public RestaurantOrder getOpenByTable(@PathVariable Long tableId) {
        return service.getOpenByTable(tableId);
    }

    @GetMapping("/orders/{id}")
    public RestaurantOrder get(@PathVariable Long id) {
        return service.get(id);
    }

    @PostMapping("/orders/{id}/items")
    public RestaurantOrder addItem(@PathVariable Long id, @Valid @RequestBody ItemRequest request) {
        return service.addItem(id, request.productId(), request.quantity());
    }

    @PatchMapping("/orders/{orderId}/items/{itemId}")
    public RestaurantOrder updateItemIngredients(
        @PathVariable Long orderId,
        @PathVariable Long itemId,
        @Valid @RequestBody ItemModificationRequest request
    ) {
        return service.updateItemIngredients(orderId, itemId, request.removedIngredients());
    }

    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    public RestaurantOrder removeItem(@PathVariable Long orderId, @PathVariable Long itemId) {
        return service.removeItem(orderId, itemId);
    }

    @PostMapping("/orders/{id}/kitchen-batches")
    @ResponseStatus(HttpStatus.CREATED)
    public RestaurantOrder submitToKitchen(@PathVariable Long id) {
        return service.submitToKitchen(id);
    }

    @GetMapping("/kitchen/order-batches")
    public List<OrderService.KitchenTicketView> kitchenOrderTickets() {
        return service.kitchenOrderTickets();
    }

    @PatchMapping("/kitchen/order-batches/{batchId}/advance")
    public RestaurantOrder advanceKitchenBatch(@PathVariable String batchId) {
        return service.advanceKitchenBatch(batchId);
    }

    @PatchMapping("/orders/{orderId}/kitchen-batches/{batchId}/deliver")
    public RestaurantOrder deliverKitchenBatch(@PathVariable Long orderId, @PathVariable String batchId) {
        return service.deliverKitchenBatch(orderId, batchId);
    }

    @PostMapping("/orders/{id}/close")
    @ResponseStatus(HttpStatus.CREATED)
    public Invoice close(@PathVariable Long id, @Valid @RequestBody CloseRequest request) {
        return service.close(id, request.paymentMethod());
    }

    public record ItemRequest(@NotNull Long productId, @NotNull @Min(1) Integer quantity) {}
    public record ItemModificationRequest(@Size(max = 500) String removedIngredients) {}
    public record CloseRequest(@NotNull PaymentMethod paymentMethod) {}
}

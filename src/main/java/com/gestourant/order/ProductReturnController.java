package com.gestourant.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices/{invoiceId}/returns")
public class ProductReturnController {
    private final ProductReturnService returns;

    public ProductReturnController(ProductReturnService returns) {
        this.returns = returns;
    }

    @GetMapping
    public ReturnHistoryResponse history(@PathVariable Long invoiceId) {
        ProductReturnService.ReturnHistory history = returns.history(invoiceId);
        return new ReturnHistoryResponse(
            history.invoiceId(),
            history.returnedQuantities(),
            history.returns().stream().map(this::response).toList()
        );
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReturnResponse create(
        @PathVariable Long invoiceId,
        @Valid @RequestBody ReturnRequest request
    ) {
        return response(returns.create(invoiceId, new ProductReturnService.ReturnCommand(
            request.orderItemId(),
            request.quantity(),
            request.reason(),
            request.notes(),
            request.restocked(),
            request.refundMethod()
        )));
    }

    private ReturnResponse response(ProductReturnService.ReturnDetail detail) {
        boolean includeCost = SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
        return new ReturnResponse(
            detail.id(),
            detail.orderItemId(),
            detail.productName(),
            detail.quantity(),
            detail.unitPrice(),
            detail.refundAmount(),
            detail.restocked(),
            includeCost && detail.lossAmount().signum() > 0 ? detail.lossAmount() : null,
            detail.reason(),
            detail.notes(),
            detail.refundMethod(),
            detail.processedBy(),
            detail.returnedAt()
        );
    }

    public record ReturnHistoryResponse(
        Long invoiceId,
        java.util.Map<Long, Integer> returnedQuantities,
        java.util.List<ReturnResponse> returns
    ) {}

    public record ReturnResponse(
        Long id,
        Long orderItemId,
        String productName,
        int quantity,
        java.math.BigDecimal unitPrice,
        java.math.BigDecimal refundAmount,
        boolean restocked,
        java.math.BigDecimal lossAmount,
        String reason,
        String notes,
        String refundMethod,
        String processedBy,
        java.time.LocalDateTime returnedAt
    ) {}

    public record ReturnRequest(
        @NotNull Long orderItemId,
        @Min(1) int quantity,
        @NotBlank @Pattern(regexp = "CALIDAD|ERROR_PEDIDO|CLIENTE_INSATISFECHO|OTRO") String reason,
        @Size(max = 500) String notes,
        boolean restocked,
        @NotNull PaymentMethod refundMethod
    ) {}
}

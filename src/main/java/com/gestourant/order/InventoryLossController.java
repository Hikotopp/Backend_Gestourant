package com.gestourant.order;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/inventory/losses")
public class InventoryLossController {
    private final InventoryLossService losses;

    public InventoryLossController(InventoryLossService losses) {
        this.losses = losses;
    }

    @GetMapping
    public LossReportResponse list(@RequestParam LocalDate from, @RequestParam LocalDate to) {
        InventoryLossService.LossReport report = losses.list(from, to);
        boolean includeCosts = isAdministrator();
        return new LossReportResponse(
            report.from(),
            report.to(),
            includeCosts ? report.totalCost() : null,
            report.entries().stream().map(entry -> LossResponse.from(entry, includeCosts)).toList()
        );
    }

    @PostMapping
    public LossResponse create(@Valid @RequestBody LossRequest request) {
        return LossResponse.from(
            losses.record(request.productId(), request.quantity(), request.reason(), request.notes()),
            isAdministrator()
        );
    }

    private boolean isAdministrator() {
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRADOR"));
    }

    public record LossRequest(
        @NotNull Long productId,
        @Min(1) int quantity,
        @NotBlank @Pattern(regexp = "MERMA|CADUCIDAD|DANIO|CONTAMINACION|OTRO") String reason,
        @Size(max = 500) String notes
    ) {}

    public record LossReportResponse(
        LocalDate from,
        LocalDate to,
        BigDecimal totalCost,
        List<LossResponse> entries
    ) {}

    public record LossResponse(
        Long id,
        Long productId,
        String productName,
        int quantity,
        String reason,
        String notes,
        BigDecimal unitCost,
        BigDecimal totalCost,
        String reportedBy,
        LocalDateTime reportedAt
    ) {
        static LossResponse from(InventoryLossService.LossDetail loss, boolean includeCosts) {
            return new LossResponse(
                loss.id(),
                loss.productId(),
                loss.productName(),
                loss.quantity(),
                loss.reason(),
                loss.notes(),
                includeCosts ? loss.unitCost() : null,
                includeCosts ? loss.totalCost() : null,
                loss.reportedBy(),
                loss.reportedAt()
            );
        }
    }
}

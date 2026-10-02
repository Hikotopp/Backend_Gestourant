package com.gestourant.report;

import com.gestourant.order.OrderService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final OrderService orders;

    public ReportController(OrderService orders) {
        this.orders = orders;
    }

    @GetMapping("/cash-close")
    public OrderService.CashClose cashClose(@RequestParam(required = false) LocalDate date) {
        return orders.cashClose(date == null ? LocalDate.now(ZoneId.of("America/Bogota")) : date);
    }

    @GetMapping("/history")
    public OrderService.InvoiceHistory employeeHistory(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
    ) {
        return orders.employeeInvoiceHistory(from, to);
    }

    @GetMapping("/history/all")
    @PreAuthorize("hasRole('ADMINISTRADOR')")
    public OrderService.InvoiceHistory allHistory(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
    ) {
        return orders.allInvoiceHistory(from, to);
    }
}

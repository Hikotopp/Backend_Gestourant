package com.gestourant.order;

import com.gestourant.catalog.ProductRepository;
import com.gestourant.restaurant.RestaurantTableRepository;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class OrderServiceReportTest {
    private final RestaurantTableRepository tables = mock(RestaurantTableRepository.class);
    private final RestaurantOrderRepository orders = mock(RestaurantOrderRepository.class);
    private final ProductRepository products = mock(ProductRepository.class);
    private final InvoiceRepository invoices = mock(InvoiceRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final GuestOrderRequestRepository guestRequests = mock(GuestOrderRequestRepository.class);
    private final OrderService service = new OrderService(tables, orders, products, invoices, users, guestRequests);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void employeeHistoryIsScopedToTheAuthenticatedEmployeeAndInclusiveDates() {
        User employee = mock(User.class);
        when(employee.getId()).thenReturn(17L);
        when(users.findByEmailIgnoreCase("employee@example.com")).thenReturn(Optional.of(employee));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("employee@example.com", "token")
        );
        when(invoices.findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
            LocalDate.of(2026, 10, 1).atStartOfDay(),
            LocalDate.of(2026, 10, 4).atStartOfDay(),
            17L
        )).thenReturn(List.of());

        OrderService.InvoiceHistory history = service.employeeInvoiceHistory(
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 10, 3)
        );

        assertEquals(0, history.facturas());
        assertEquals(LocalDate.of(2026, 10, 1), history.from());
        assertEquals(LocalDate.of(2026, 10, 3), history.to());
        verify(invoices).findByIssuedAtGreaterThanEqualAndIssuedAtLessThanAndOrder_CreatedBy_IdOrderByIssuedAtAsc(
            LocalDate.of(2026, 10, 1).atStartOfDay(),
            LocalDate.of(2026, 10, 4).atStartOfDay(),
            17L
        );
    }

    @Test
    void rejectsAnInvertedDateRangeWithoutQueryingInvoices() {
        assertThrows(IllegalArgumentException.class, () -> service.allInvoiceHistory(
            LocalDate.of(2026, 10, 3),
            LocalDate.of(2026, 10, 1)
        ));

        verifyNoInteractions(invoices);
    }
}

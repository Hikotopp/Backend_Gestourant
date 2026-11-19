package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InventoryLossServiceTest {
    private final ProductCatalogPort products = mock(ProductCatalogPort.class);
    private final InventoryLossRepository losses = mock(InventoryLossRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final InventoryLossService service = new InventoryLossService(products, losses, users);

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void recordsLossAgainstStockAndSnapshotsItsCost() {
        Product product = new Product("Jugo", "Natural", "BEBIDA", null, new BigDecimal("9000"), new BigDecimal("2500"), 5);
        User employee = mock(User.class);
        when(employee.getRole()).thenReturn(Role.EMPLEADO);
        when(users.findByEmailIgnoreCase("cashier@example.com")).thenReturn(Optional.of(employee));
        when(products.findById(4L)).thenReturn(Optional.of(product));
        when(losses.save(any(InventoryLoss.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("cashier@example.com", "token")
        );

        InventoryLossService.LossDetail result = service.record(4L, 2, "CADUCIDAD", "Vencido");

        assertEquals(3, product.getStock());
        assertEquals(0, new BigDecimal("5000.00").compareTo(result.totalCost()));
        verify(products).save(product);
        verify(losses).save(any(InventoryLoss.class));
    }

    @Test
    void doesNotPersistLossWhenQuantityExceedsAvailableStock() {
        Product product = new Product("Jugo", "Natural", "BEBIDA", new BigDecimal("9000"), 1);
        when(products.findById(4L)).thenReturn(Optional.of(product));

        assertThrows(IllegalArgumentException.class, () -> service.record(4L, 2, "CADUCIDAD", ""));

        assertEquals(1, product.getStock());
        verifyNoInteractions(losses);
    }
}

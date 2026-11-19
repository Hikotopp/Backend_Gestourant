package com.gestourant.order;

import com.gestourant.catalog.Product;
import com.gestourant.catalog.application.port.out.ProductCatalogPort;
import com.gestourant.user.Role;
import com.gestourant.user.User;
import com.gestourant.user.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class InventoryLossService {
    private final ProductCatalogPort products;
    private final InventoryLossRepository losses;
    private final UserRepository users;

    public InventoryLossService(
        ProductCatalogPort products,
        InventoryLossRepository losses,
        UserRepository users
    ) {
        this.products = products;
        this.losses = losses;
        this.users = users;
    }

    @Transactional
    public LossReport list(LocalDate from, LocalDate to) {
        validateDates(from, to);
        User user = currentUser();
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<InventoryLoss> entries = user.getRole() == Role.ADMINISTRADOR
            ? losses.findByReportedAtGreaterThanEqualAndReportedAtLessThanOrderByReportedAtDesc(start, end)
            : losses.findByReportedAtGreaterThanEqualAndReportedAtLessThanAndReportedBy_IdOrderByReportedAtDesc(
                start,
                end,
                user.getId()
            );
        BigDecimal totalCost = entries.stream()
            .map(InventoryLoss::getTotalCost)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new LossReport(from, to, totalCost, entries.stream().map(LossDetail::from).toList());
    }

    @Transactional
    public LossDetail record(Long productId, int quantity, String reason, String notes) {
        if (quantity < 1) throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        Product product = products.findById(productId)
            .orElseThrow(() -> new IllegalArgumentException("Producto no encontrado"));
        if (product.getUnitCost() == null || product.getUnitCost().signum() <= 0) {
            throw new IllegalArgumentException("Configura el costo unitario interno del producto antes de registrar la pérdida");
        }
        product.reduceStock(quantity);
        products.save(product);
        InventoryLoss loss = new InventoryLoss(
            product,
            currentUser(),
            quantity,
            reason,
            notes == null ? "" : notes.trim()
        );
        return LossDetail.from(losses.save(loss));
    }

    private void validateDates(LocalDate from, LocalDate to) {
        if (from == null || to == null) {
            throw new IllegalArgumentException("Selecciona una fecha inicial y final");
        }
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("La fecha final no puede ser anterior a la inicial");
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return users.findByEmailIgnoreCase(email)
            .orElseThrow(() -> new IllegalStateException("Usuario no encontrado"));
    }

    public record LossReport(LocalDate from, LocalDate to, BigDecimal totalCost, List<LossDetail> entries) {}

    public record LossDetail(
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
        static LossDetail from(InventoryLoss loss) {
            return new LossDetail(
                loss.getId(),
                loss.getProduct().getId(),
                loss.getProductName(),
                loss.getQuantity(),
                loss.getReason(),
                loss.getNotes(),
                loss.getUnitCost(),
                loss.getTotalCost(),
                loss.getReportedBy().getUsername(),
                loss.getReportedAt()
            );
        }
    }
}

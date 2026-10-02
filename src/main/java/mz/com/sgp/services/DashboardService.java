package mz.com.sgp.services;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.data.dto.DashboardSummaryDTO;
import mz.com.sgp.model.SaleStatus;
import mz.com.sgp.repository.ProductRepository;
import mz.com.sgp.repository.SaleRepository;

@Service
public class DashboardService {
    private final SaleRepository sales;
    private final ProductRepository products;

    public DashboardService(SaleRepository sales, ProductRepository products) {
        this.sales = sales;
        this.products = products;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO summary() {
        return summaryAt(LocalDateTime.now());
    }

    DashboardSummaryDTO summaryAt(LocalDateTime now) {
        var today = now.toLocalDate().atStartOfDay();
        var month = today.withDayOfMonth(1);
        var previousEnd = now.minusMonths(1);
        var threshold = new BigDecimal("5");
        return new DashboardSummaryDTO(
            sales.sumRevenue(today, now, SaleStatus.COMPLETED, EntityState.ACTIVE),
            sales.sumRevenue(month, now, SaleStatus.COMPLETED, EntityState.ACTIVE),
            count(today, now), count(today.minusDays(1), now.minusDays(1)),
            count(month, now), count(month.minusMonths(1), previousEnd),
            products.countLowStock(EntityState.ACTIVE, threshold), threshold, now, previousEnd);
    }

    private long count(LocalDateTime start, LocalDateTime end) {
        return sales.countSalesCurrentMonth(start, end, SaleStatus.COMPLETED, EntityState.ACTIVE);
    }
}

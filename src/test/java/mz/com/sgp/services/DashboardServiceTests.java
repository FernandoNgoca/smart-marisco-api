package mz.com.sgp.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.SaleStatus;
import mz.com.sgp.repository.ProductRepository;
import mz.com.sgp.repository.SaleRepository;

class DashboardServiceTests {
    @Test void comparesElapsedPeriodsAndClampsShorterMonths() {
        var sales = mock(SaleRepository.class);
        var products = mock(ProductRepository.class);
        var now = LocalDateTime.of(2026, 3, 31, 14, 30);
        var start = LocalDateTime.of(2026, 3, 1, 0, 0);
        when(sales.sumRevenue(start, now, SaleStatus.COMPLETED, EntityState.ACTIVE))
            .thenReturn(new BigDecimal("123.45"));
        when(products.countLowStock(EntityState.ACTIVE, new BigDecimal("5"))).thenReturn(3L);
        var result = new DashboardService(sales, products).summaryAt(now);
        assertThat(result.revenueMonth()).isEqualByComparingTo("123.45");
        assertThat(result.lowStockProducts()).isEqualTo(3);
        assertThat(result.previousPeriodEnd()).isEqualTo(LocalDateTime.of(2026, 2, 28, 14, 30));
        verify(sales).countSalesCurrentMonth(LocalDateTime.of(2026, 2, 1, 0, 0),
            LocalDateTime.of(2026, 2, 28, 14, 30), SaleStatus.COMPLETED, EntityState.ACTIVE);
        verify(sales).countSalesCurrentMonth(LocalDateTime.of(2026, 3, 30, 0, 0),
            LocalDateTime.of(2026, 3, 30, 14, 30), SaleStatus.COMPLETED, EntityState.ACTIVE);
    }
}

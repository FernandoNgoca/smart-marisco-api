package mz.com.sgp.services;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import mz.com.sgp.model.SaleEntity;
import mz.com.sgp.model.SaleStatus;
import mz.com.sgp.repository.SaleRepository;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:dashboard;MODE=MySQL;DB_CLOSE_DELAY=-1")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class DashboardIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired SaleRepository sales;

    @Test void summaryExecutesQueriesAndSerializesRevenue() throws Exception {
        var sale = new SaleEntity();
        sale.setSaleStatus(SaleStatus.COMPLETED);
        sale.setTotalValue(new BigDecimal("48884.00"));
        sales.saveAndFlush(sale);
        mvc.perform(get("/api/dashboard/v1/summary").with(user("manager").roles("MANAGER")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.revenueToday").value(48884.00))
            .andExpect(jsonPath("$.revenueMonth").value(48884.00))
            .andExpect(jsonPath("$.salesToday").value(1))
            .andExpect(jsonPath("$.lowStockProducts").value(0))
            .andExpect(jsonPath("$.updatedAt").isString())
            .andExpect(jsonPath("$.updatedAt").value(org.hamcrest.Matchers.matchesPattern("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}")))
            .andExpect(jsonPath("$.previousPeriodEnd").isString());
    }

    @Test void summaryDeniesAdminWithoutManagerRole() throws Exception {
        mvc.perform(get("/api/dashboard/v1/summary").with(user("admin").roles("ADMIN")))
            .andExpect(status().isForbidden());
    }

    @Test void administratorOnlyManagesUsers() throws Exception {
        mvc.perform(get("/auth").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk());
        for (String path : new String[]{"/api/product/v1", "/api/stock/v1", "/api/client/v1", "/api/sale/v1", "/api/category/v1"}) {
            mvc.perform(get(path).with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .with(user("admin").roles("ADMIN")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        }
    }

    @Test void administratorCannotInheritBusinessAccessFromOtherRoles() throws Exception {
        for (String path : new String[]{"/api/dashboard/v1/summary", "/api/client/v1", "/api/sale/v1", "/api/product/v1", "/api/stock/v1"}) {
            mvc.perform(get(path).with(user("admin").roles("ADMIN", "MANAGER", "USER")))
                .andExpect(status().isForbidden());
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(path)
                .with(user("admin").roles("ADMIN", "MANAGER", "USER")).contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
        }
    }

    @Test void summaryDeniesOrdinaryUsers() throws Exception {
        mvc.perform(get("/api/dashboard/v1/summary").with(user("seller").roles("USER")))
            .andExpect(status().isForbidden());
    }
}

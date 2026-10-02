package mz.com.sgp.security;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import mz.com.sgp.services.StockServices;
import mz.com.sgp.services.StockMovementServices;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class StockAccessTests {
    @Autowired MockMvc mvc;
    @MockBean StockServices stocks;
    @MockBean StockMovementServices movements;

    @Test void usersAndManagersCanManageStock() throws Exception {
        for (String role : new String[]{"USER", "MANAGER"}) {
            mvc.perform(post("/api/stock/v1").with(user("tester").roles(role))
                .contentType("application/json").content("{\"productId\":1,\"quantity\":5}"))
                .andExpect(status().isOk());
            mvc.perform(put("/api/stock/v1").with(user("tester").roles(role))
                .contentType("application/json").content("{\"id\":1,\"quantity\":6}"))
                .andExpect(status().isOk());
            mvc.perform(post("/api/stockMovement/v1").with(user("tester").roles(role))
                .contentType("application/json").content("{\"stockId\":1,\"quantity\":1}"))
                .andExpect(status().isOk());
        }
        verify(stocks, times(2)).create(any());
        verify(stocks, times(2)).update(any());
        verify(movements, times(2)).create(any());
    }

    @Test void administratorStillCannotManageStockEvenWithOtherRoles() throws Exception {
        mvc.perform(post("/api/stock/v1").with(user("admin").roles("ADMIN", "USER", "MANAGER"))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(put("/api/stock/v1").with(user("admin").roles("ADMIN", "USER"))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/api/stockMovement/v1").with(user("admin").roles("ADMIN", "USER"))
            .contentType("application/json").content("{}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(stocks, movements);
    }
}

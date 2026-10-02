package mz.com.sgp.services;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleSortingTests {
    @Autowired MockMvc mvc;

    @Test void historiesAcceptSupportedColumnsAndSafelyHandleInvalidFields() throws Exception {
        for (String endpoint : new String[]{"/api/sale/v1", "/api/sale/v1/findAllOrders"}) {
            for (String field : new String[]{"", "name", "firstName", "lastName", "phoneNumber", "date", "createdDate", "totalValue", "saleStatus", "id", "unknown"}) {
                mvc.perform(get(endpoint).param("sortField", field).with(user("manager").roles("MANAGER")))
                    .andExpect(status().isOk());
            }
        }
    }
}

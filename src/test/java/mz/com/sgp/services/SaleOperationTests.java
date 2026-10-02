package mz.com.sgp.services;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import mz.com.sgp.data.dto.*;
import mz.com.sgp.model.*;
import mz.com.sgp.repository.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SaleOperationTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired SaleOperationService service;
    @Autowired SaleOperationRepository operations;
    @Autowired UserRepository users;
    @Autowired SaleRepository sales;
    @MockBean SaleServices creator;
    String username;
    String key;

    @BeforeEach void setup() {
        username = "u" + UUID.randomUUID().toString().substring(0, 12);
        key = UUID.randomUUID().toString();
        UserEntity user = new UserEntity();
        user.setUserName(username);
        user.setFullName(username);
        user.setPassword("unused-test-password");
        user.setEnabled(true);
        user.setAccountNonExpired(true);
        user.setAccountNonLocked(true);
        user.setCredentialsNonExpired(true);
        users.save(user);
        when(creator.create(any(), any())).thenAnswer(invocation -> {
            SaleEntity entity = new SaleEntity();
            entity.setTotalValue(BigDecimal.TEN);
            entity.setSaleStatus(SaleStatus.COMPLETED);
            SaleDTO dto = new SaleDTO();
            dto.setId(sales.save(entity).getId());
            dto.setTotalValue(BigDecimal.TEN);
            return dto;
        });
    }

    SaleRequestDTO request() {
        SaleDTO sale = new SaleDTO();
        sale.setTotalValue(BigDecimal.TEN);
        sale.setSaleStatus(SaleStatus.COMPLETED);
        SaleItemDTO item = new SaleItemDTO();
        item.setProductId(1L);
        item.setQuantity(BigDecimal.ONE);
        SaleRequestDTO request = new SaleRequestDTO();
        request.setSale(sale);
        request.setItems(List.of(item));
        return request;
    }

    @Test void httpContractSupportsRetriesAndRejectsChangedPayload() throws Exception {
        String body = mapper.writeValueAsString(request());
        var first = mvc.perform(post("/api/sale/v1").with(user(username).roles("USER"))
                .header("Idempotency-Key", key).contentType("application/json").content(body))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        mvc.perform(post("/api/sale/v1").with(user(username).roles("USER"))
                .header("Idempotency-Key", key).contentType("application/json").content(body))
                .andExpect(status().isOk()).andExpect(content().json(first));
        var changed = request();
        changed.getItems().get(0).setQuantity(BigDecimal.TEN);
        mvc.perform(post("/api/sale/v1").with(user(username).roles("USER"))
                .header("Idempotency-Key", key).contentType("application/json").content(mapper.writeValueAsString(changed)))
                .andExpect(status().isConflict());
        mvc.perform(post("/api/sale/v1").with(user(username).roles("USER"))
                .contentType("application/json").content(body)).andExpect(status().isBadRequest());
        verify(creator, times(1)).create(any(), any());
    }

    @Test void corsAllowsSaleOperationHeader() throws Exception {
        mvc.perform(options("/api/sale/v1").header("Origin", "http://localhost:5173")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Authorization,Content-Type,Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Headers", org.hamcrest.Matchers.containsString("Idempotency-Key")));
    }

    @Test void retryReturnsOriginalSaleWithoutSecondWrite() {
        var first = service.create(username, key, request());
        var second = service.create(username, key, request());
        assertThat(second.getId()).isEqualTo(first.getId());
        verify(creator, times(1)).create(any(), any());
    }

    @Test void concurrentRetriesOnlyCreateOnce() throws Exception {
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<SaleDTO> work = () -> { start.await(); return service.create(username, key, request()); };
            var first = executor.submit(work);
            var second = executor.submit(work);
            start.countDown();
            assertThat(first.get(15, TimeUnit.SECONDS).getId()).isEqualTo(second.get(15, TimeUnit.SECONDS).getId());
        }
        verify(creator, times(1)).create(any(), any());
    }

    @Test void differentPayloadWithSameKeyIsConflict() {
        service.create(username, key, request());
        var changed = request();
        changed.getItems().get(0).setQuantity(BigDecimal.TEN);
        assertThatThrownBy(() -> service.create(username, key, changed))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode().value()).isEqualTo(409));
        verify(creator, times(1)).create(any(), any());
    }

    @Test void failedSaleRollsBackAndCanBeRetried() {
        long before = sales.count();
        doAnswer(invocation -> {
            SaleEntity entity = new SaleEntity();
            entity.setTotalValue(BigDecimal.TEN);
            entity.setSaleStatus(SaleStatus.COMPLETED);
            sales.saveAndFlush(entity);
            throw new IllegalStateException("stock unavailable");
        }).when(creator).create(any(), any());
        assertThatThrownBy(() -> service.create(username, key, request())).isInstanceOf(IllegalStateException.class);
        assertThat(sales.count()).isEqualTo(before);
        assertThat(operations.findByUserIdAndRequestKey(users.findByUsername(username).getId(), key)).isEmpty();
    }

    @Test void rejectsMissingKeyAndInvalidQuantityBeforeWriting() {
        assertThatThrownBy(() -> service.create(username, null, request())).isInstanceOf(ResponseStatusException.class);
        var invalid = request();
        invalid.getItems().get(0).setQuantity(BigDecimal.ZERO);
        assertThatThrownBy(() -> service.create(username, key, invalid)).isInstanceOf(ResponseStatusException.class);
        verifyNoInteractions(creator);
    }
}

package mz.com.sgp.services;

import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import jakarta.persistence.EntityManager;
import mz.com.sgp.model.*;
import mz.com.sgp.data.dto.*;
import mz.com.sgp.repository.*;

@SpringBootTest
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class OrderLifecycleTests {
    @Autowired org.springframework.test.web.servlet.MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired OrderLifecycleService service;
    @Autowired StockRepository stocks;
    @Autowired SaleRepository sales;
    @Autowired StockMovementRepository movements;
    ProductEntity product;
    ClientEntity client;
    StockEntity stock;

    @BeforeEach void setup() {
        var category = new CategoryEntity(); category.setName("Marisco"); category.setDescription("Teste"); em.persist(category);
        var species = new SpeciesEntity(); species.setName("Camarão"); species.setDescription("Teste"); species.setCategoryId(category.getId()); em.persist(species);
        var unit = new UnitEntity(); unit.setName("Quilo"); unit.setSymbol("kg-test"); em.persist(unit);
        product = new ProductEntity(); product.setName("Camarão"); product.setDescription("Teste"); product.setCode("test-order");
        product.setPrice(BigDecimal.ONE); product.setSalePrice(new BigDecimal("100.00")); product.setSpeciesId(species.getId()); product.setUnitId(unit.getId()); em.persist(product);
        client = new ClientEntity(); client.setFirstName("Cliente"); client.setPhoneNumber("840000000"); client.setType(ClientType.values()[0]); em.persist(client);
        stock = new StockEntity(); stock.setProductId(product.getId()); stock.setQuantity(new BigDecimal("10")); em.persist(stock); em.flush();
    }
    SaleDTO create(String quantity) {
        var sale = new SaleDTO(); sale.setSaleStatus(SaleStatus.ORDERS); sale.setClientId(client.getId()); sale.setTotalValue(BigDecimal.ONE);
        var line = new SaleItemDTO(); line.setProductId(product.getId()); line.setQuantity(new BigDecimal(quantity)); line.setUnitPrice(BigDecimal.ONE);
        return service.create(sale, List.of(line));
    }
    @Test void registrationDoesNotReserveAndCompletionDeductsOnlyOnce() {
        var order = create("3");
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
        assertThat(movements.count()).isZero();
        assertThat(order.getTotalValue()).isEqualByComparingTo("300");
        product.setSalePrice(new BigDecimal("200")); em.flush();
        var detail = service.detail(order.getId());
        assertThat(detail.getItems().get(0).getUnitPrice()).isEqualByComparingTo("100");
        var completed = service.complete(order.getId(), order.getVersion());
        assertThat(completed.getSaleStatus()).isEqualTo(SaleStatus.COMPLETED);
        assertThat(stock.getQuantity()).isEqualByComparingTo("7");
        service.complete(order.getId(), order.getVersion());
        assertThat(stock.getQuantity()).isEqualByComparingTo("7");
        assertThat(movements.count()).isEqualTo(1);
    }
    @Test void insufficientStockLeavesOrderPending() {
        var order = create("20");
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
        assertThatThrownBy(() -> service.complete(order.getId(), order.getVersion())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("Stock insuficiente");
        assertThat(sales.findById(order.getId()).orElseThrow().getSaleStatus()).isEqualTo(SaleStatus.ORDERS);
        assertThat(movements.count()).isZero();
    }
    @Test void cancellationDoesNotChangeStockAndCannotBeCompleted() {
        var order = create("2");
        service.cancel(order.getId(), order.getVersion());
        service.cancel(order.getId(), order.getVersion());
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
        assertThat(movements.count()).isZero();
        assertThatThrownBy(() -> service.complete(order.getId(), order.getVersion())).isInstanceOf(ResponseStatusException.class);
    }
    @Test void editingKeepsAgreedPriceAndRejectsStaleVersion() {
        var order = create("2"); var request = service.detail(order.getId());
        product.setSalePrice(new BigDecimal("200"));
        request.getItems().get(0).setQuantity(new BigDecimal("3"));
        var updated = service.update(order.getId(), request);
        assertThat(updated.getTotalValue()).isEqualByComparingTo("300");
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
        assertThatThrownBy(() -> service.update(order.getId(), request)).isInstanceOf(ResponseStatusException.class).hasMessageContaining("alterado");
    }
    @Test void clientIsMandatoryForOrders() {
        var request = new SaleDTO(); request.setSaleStatus(SaleStatus.ORDERS);
        assertThatThrownBy(() -> service.create(request, List.of())).isInstanceOf(ResponseStatusException.class).hasMessageContaining("cliente");
    }
    @Test void legacyCancellationReturnsPreviouslyDeductedStockOnlyOnce() {
        var order = create("2");
        sales.findById(order.getId()).orElseThrow().setStockDeducted(true); stock.setQuantity(new BigDecimal("8")); em.flush();
        var version = sales.findById(order.getId()).orElseThrow().getVersion();
        service.cancel(order.getId(), version); service.cancel(order.getId(), version);
        assertThat(stock.getQuantity()).isEqualByComparingTo("10");
        assertThat(movements.count()).isEqualTo(1);
    }
    @Test void httpCompletionReturnsReadableStockConflict() throws Exception {
        var order = create("20");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/sale/v1/orders/" + order.getId() + "/complete")
            .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller").roles("USER"))
            .contentType("application/json").content("{\"version\":" + order.getVersion() + "}"))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isConflict())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Stock insuficiente")));
    }

    @Autowired StockMovementServices historyService;

    @Test void stockHistoryPaginatesAndKeepsTotalsIndependentOfFilters() throws Exception {
        for (int i = 0; i < 15; i++) {
            var movement = new StockMovementEntity();
            movement.setStockId(stock.getId());
            movement.setType(i < 13 ? MovementType.ENTRY : MovementType.EXIT);
            movement.setQuantity(new BigDecimal("2.500"));
            movement.setDescription("Teste de histórico");
            em.persist(movement);
        }
        em.flush();
        var first = historyService.history(product.getId(), 0, 5, null, null, null);
        var second = historyService.history(product.getId(), 1, 5, null, null, null);
        assertThat(first.items()).hasSize(5);
        assertThat(first.totalElements()).isEqualTo(15);
        assertThat(first.movements()).isEqualTo(15);
        assertThat(first.entries()).isEqualByComparingTo("32.5");
        assertThat(first.exits()).isEqualByComparingTo("5");
        assertThat(first.items().stream().map(StockMovementDTO::getId).toList())
            .doesNotContainAnyElementsOf(second.items().stream().map(StockMovementDTO::getId).toList());
        var exits = historyService.history(product.getId(), 0, 5, MovementType.EXIT, null, null);
        assertThat(exits.totalElements()).isEqualTo(2);
        assertThat(exits.entries()).isEqualByComparingTo("32.5");
        var future = historyService.history(product.getId(), 0, 5, null, java.time.LocalDate.now().plusDays(1), null);
        assertThat(future.items()).isEmpty();
        assertThat(future.movements()).isEqualTo(15);
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/stockMovement/v1/product/" + product.getId() + "/history")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller").roles("USER")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.totalElements").value(15))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.items[0].description").value("Teste de histórico"));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/api/stockMovement/v1/product/" + product.getId() + "/history?from=2026-10-02&to=2026-10-01")
                .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("seller").roles("USER")))
            .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isBadRequest());
    }

    @Test void completedOrderRecordsMovementReference() {
        var order = create("1");
        service.complete(order.getId(), order.getVersion());
        em.flush();
        var history = historyService.history(product.getId(), 0, 5, null, null, null);
        assertThat(history.items()).hasSize(1);
        assertThat(history.items().get(0).getDescription()).isEqualTo("Pedido #" + order.getId());
    }
}

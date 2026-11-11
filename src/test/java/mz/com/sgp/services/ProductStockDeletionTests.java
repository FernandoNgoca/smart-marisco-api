package mz.com.sgp.services;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.model.ProductEntity;
import mz.com.sgp.model.StockEntity;
import mz.com.sgp.repository.ProductRepository;
import mz.com.sgp.repository.StockRepository;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProductStockDeletionTests {
    @Test void rejectsPositiveFractionalAndNegativeBalancesWithoutChangingProduct() {
        for (String quantity : List.of("5", "0.001", "-1")) {
            var products = mock(ProductRepository.class);
            var stocks = mock(StockRepository.class);
            var service = new ProductServices();
            ReflectionTestUtils.setField(service, "productRepository", products);
            ReflectionTestUtils.setField(service, "stockRepository", stocks);
            var product = new ProductEntity();
            product.setId(1L);
            product.setStatus(EntityState.ACTIVE);
            var stock = new StockEntity();
            stock.setQuantity(new BigDecimal(quantity));
            when(products.lockById(1L)).thenReturn(Optional.of(product));
            when(stocks.lockAllByProductId(1L)).thenReturn(List.of(stock));
            var error = assertThrows(ResponseStatusException.class, () -> service.disableProduct(1L));
            assertEquals(409, error.getStatusCode().value());
            assertEquals(EntityState.ACTIVE, product.getStatus());
            verify(products, never()).save(any());
        }
    }

    @Test void permitsZeroBalanceOrNoStock() {
        var zero = new StockEntity();
        zero.setQuantity(BigDecimal.ZERO);
        for (List<StockEntity> entries : List.of(List.<StockEntity>of(), List.of(zero))) {
            var products = mock(ProductRepository.class);
            var stocks = mock(StockRepository.class);
            var service = new ProductServices();
            ReflectionTestUtils.setField(service, "productRepository", products);
            ReflectionTestUtils.setField(service, "stockRepository", stocks);
            var product = new ProductEntity();
            product.setId(1L);
            product.setStatus(EntityState.ACTIVE);
            when(products.lockById(1L)).thenReturn(Optional.of(product));
            when(stocks.lockAllByProductId(1L)).thenReturn(entries);
            service.disableProduct(1L);
            assertEquals(EntityState.INACTIVE, product.getStatus());
            verify(products).save(product);
        }
    }
}

package mz.com.sgp.services;

import static mz.com.sgp.mapper.ObjectMapper.parseObject;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import mz.com.sgp.config.audit.entity.EntityState;
import mz.com.sgp.data.dto.*;
import mz.com.sgp.model.*;
import mz.com.sgp.repository.*;
import mz.com.sgp.validation.QuantityRules;

@Service
@Transactional
public class OrderLifecycleService {
    private final SaleRepository sales;
    private final SaleItemRepository items;
    private final ProductRepository products;
    private final ClientRepository clients;
    private final StockRepository stocks;
    private final StockMovementServices movements;

    public OrderLifecycleService(SaleRepository sales, SaleItemRepository items, ProductRepository products,
            ClientRepository clients, StockRepository stocks, StockMovementServices movements) {
        this.sales = sales; this.items = items; this.products = products;
        this.clients = clients; this.stocks = stocks; this.movements = movements;
    }

    public SaleDTO create(SaleDTO input, List<SaleItemDTO> requested) {
        if (input == null || (input.getSaleStatus() != SaleStatus.ORDERS && input.getSaleStatus() != SaleStatus.COMPLETED))
            throw error(HttpStatus.BAD_REQUEST, "Estado inicial inválido");
        var sale = new SaleEntity();
        sale.setSaleStatus(input.getSaleStatus());
        sale.setOrderRecord(input.getSaleStatus() == SaleStatus.ORDERS);
        setClient(sale, input.getClientId());
        var lines = prepare(requested, List.of());
        sale.setTotalValue(total(lines));
        sales.saveAndFlush(sale);
        for (var line : lines) line.setSaleId(sale.getId());
        items.saveAll(lines);
        if (!sale.getOrderRecord()) {
            moveStock(lines, MovementType.EXIT, (sale.getOrderRecord() ? "Pedido #" : "Venda #") + sale.getId());
            sale.setStockDeducted(true);
            sale.setCompletedDate(LocalDateTime.now());
        }
        return dto(sale);
    }

    public SaleRequestDTO detail(Long id) {
        var sale = order(id);
        var result = new SaleRequestDTO();
        result.setSale(parseObject(sale, SaleDTO.class));
        result.setItems(lines(id).stream().map(line -> parseObject(line, SaleItemDTO.class)).toList());
        return result;
    }

    public SaleDTO update(Long id, SaleRequestDTO request) {
        var sale = order(id);
        requirePending(sale);
        if (request == null || request.getSale() == null) throw error(HttpStatus.BAD_REQUEST, "Pedido obrigatório");
        requireVersion(sale, request.getSale().getVersion());
        var oldLines = lines(id);
        var newLines = prepare(request.getItems(), oldLines);
        setClient(sale, request.getSale().getClientId());
        sale.setTotalValue(total(newLines));
        // Convert legacy pre-deducted orders to the new no-reservation workflow on edit.
        if (sale.getStockDeducted()) {
            moveStock(oldLines, MovementType.ENTRY, "Reposição do pedido #" + id);
            sale.setStockDeducted(false);
        }
        items.deleteAll(oldLines);
        for (var line : newLines) line.setSaleId(id);
        items.saveAll(newLines);
        // Force a version increment even when only quantities change with the same total.
        sale.markEdited();
        sales.flush();
        return dto(sale);
    }

    public SaleDTO complete(Long id, Long version) {
        var sale = order(id);
        if (sale.getSaleStatus() == SaleStatus.COMPLETED) return parseObject(sale, SaleDTO.class);
        requirePending(sale);
        requireVersion(sale, version);
        var lines = lines(id);
        if (lines.isEmpty()) throw error(HttpStatus.CONFLICT, "O pedido não tem artigos");
        if (!sale.getStockDeducted()) moveStock(lines, MovementType.EXIT, (sale.getOrderRecord() ? "Pedido #" : "Venda #") + sale.getId());
        sale.setStockDeducted(true);
        sale.setSaleStatus(SaleStatus.COMPLETED);
        sale.setCompletedDate(LocalDateTime.now());
        return dto(sale);
    }

    public SaleDTO cancel(Long id, Long version) {
        var sale = order(id);
        if (sale.getSaleStatus() == SaleStatus.CANCELED) return parseObject(sale, SaleDTO.class);
        requirePending(sale);
        requireVersion(sale, version);
        if (sale.getStockDeducted()) {
            moveStock(lines(id), MovementType.ENTRY, "Cancelamento do pedido #" + id);
            sale.setStockDeducted(false);
        }
        sale.setSaleStatus(SaleStatus.CANCELED);
        return dto(sale);
    }

    private SaleEntity order(Long id) {
        var sale = sales.lockById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND, "Pedido não encontrado"));
        if (!sale.getOrderRecord()) throw error(HttpStatus.NOT_FOUND, "Este registo não é um pedido");
        return sale;
    }
    private void requirePending(SaleEntity sale) {
        if (sale.getSaleStatus() != SaleStatus.ORDERS) throw error(HttpStatus.CONFLICT, "Só é possível alterar pedidos pendentes");
    }
    private void requireVersion(SaleEntity sale, Long version) {
        if (version == null || !version.equals(sale.getVersion()))
            throw error(HttpStatus.CONFLICT, "O pedido foi alterado. Atualize a lista e tente novamente");
    }
    private void setClient(SaleEntity sale, Long id) {
        if (id == null) {
            if (sale.getOrderRecord()) throw error(HttpStatus.BAD_REQUEST, "Selecione um cliente para o pedido");
            return;
        }
        var client = clients.findById(id).filter(c -> c.getStatus() == EntityState.ACTIVE)
            .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Cliente inválido ou inativo"));
        sale.setClientId(id); sale.setClient(client);
    }
    private List<SaleItemEntity> prepare(List<SaleItemDTO> requested, List<SaleItemEntity> oldLines) {
        if (requested == null || requested.isEmpty()) throw error(HttpStatus.BAD_REQUEST, "Adicione pelo menos um artigo");
        Set<Long> seen = new HashSet<>();
        List<SaleItemEntity> result = new ArrayList<>();
        for (var input : requested) {
            if (input == null || input.getProductId() == null || !seen.add(input.getProductId()))
                throw error(HttpStatus.BAD_REQUEST, "Produto inválido ou repetido");
            QuantityRules.positive(input.getQuantity());
            var product = products.findById(input.getProductId()).filter(p -> p.getStatus() == EntityState.ACTIVE)
                .orElseThrow(() -> error(HttpStatus.BAD_REQUEST, "Produto inválido ou inativo"));
            var price = oldLines.stream().filter(i -> i.getProductId().equals(input.getProductId()))
                .map(SaleItemEntity::getUnitPrice).filter(Objects::nonNull).findFirst().orElse(product.getSalePrice());
            if (price == null || price.signum() < 0) throw error(HttpStatus.BAD_REQUEST, "Preço de produto inválido");
            var line = new SaleItemEntity();
            line.setProductId(product.getId()); line.setProduct(product);
            line.setQuantity(input.getQuantity()); line.setUnitPrice(price.setScale(2, RoundingMode.HALF_UP));
            result.add(line);
        }
        return result;
    }
    private BigDecimal total(List<SaleItemEntity> lines) {
        var total = lines.stream().map(i -> i.getUnitPrice().multiply(i.getQuantity()))
            .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (total.compareTo(new BigDecimal("99999999.99")) > 0) throw error(HttpStatus.BAD_REQUEST, "O total excede o limite permitido");
        return total;
    }
    private List<SaleItemEntity> lines(Long id) { return items.findAllByStatusAndSaleId(EntityState.ACTIVE, id); }
    private void moveStock(List<SaleItemEntity> lines, MovementType type, String description) {
        // Stable lock ordering prevents two simultaneous orders from overselling or deadlocking.
        var quantities = new TreeMap<Long, BigDecimal>();
        lines.forEach(i -> quantities.merge(i.getProductId(), i.getQuantity(), BigDecimal::add));
        quantities.forEach((productId, quantity) -> {
            var stock = stocks.lockByProductId(productId)
                .orElseThrow(() -> error(HttpStatus.CONFLICT, "Não existe stock para o produto " + productId));
            if (type == MovementType.EXIT && stock.getQuantity().compareTo(quantity) < 0)
                throw error(HttpStatus.CONFLICT, "Stock insuficiente para o produto " + productId + ". Disponível: " + stock.getQuantity());
            var movement = new StockMovementDTO();
            movement.setStockId(stock.getId()); movement.setType(type); movement.setQuantity(quantity);
            movement.setDescription(description);
            movements.create(movement);
        });
    }
    private SaleDTO dto(SaleEntity sale) { return parseObject(sales.saveAndFlush(sale), SaleDTO.class); }
    private ResponseStatusException error(HttpStatus status, String message) { return new ResponseStatusException(status, message); }
}

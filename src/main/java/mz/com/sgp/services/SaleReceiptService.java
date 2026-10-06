package mz.com.sgp.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly=true)
public class SaleReceiptService {
    private final JdbcTemplate jdbc;
    public SaleReceiptService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Item(String product, String unit, BigDecimal quantity, BigDecimal unitPrice, BigDecimal subtotal) {}
    public record Receipt(long id, String date, String client, String operator, BigDecimal total, BigDecimal roundingAdjustment, List<Item> items) {}
    public Receipt receipt(Long id) {
        var sales=jdbc.query("SELECT s.ID,COALESCE(s.COMPLETED_DATE,s.CREATED_DATE),CASE WHEN c.ID IS NULL THEN 'Consumidor final' ELSE TRIM(CONCAT(COALESCE(c.FIRST_NAME,''),' ',COALESCE(c.LAST_NAME,''))) END,COALESCE(s.CREATED_BY,'Não registado'),s.TOTAL_VALUE,s.SALE_STATUS FROM SALE s LEFT JOIN CLIENT c ON c.ID=s.CLIENT_ID WHERE s.ID=? AND s.STATUS=1",(rs,i)->{
            if (!"COMPLETED".equals(rs.getString(6))) throw new ResponseStatusException(HttpStatus.CONFLICT,"O recibo só está disponível para vendas concluídas");
            return new Receipt(rs.getLong(1),rs.getTimestamp(2).toLocalDateTime().toString(),rs.getString(3).trim(),rs.getString(4),rs.getBigDecimal(5),BigDecimal.ZERO,List.of());
        },id);
        if(sales.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Venda não encontrada");
        var sale=sales.get(0);
        var items=jdbc.query("SELECT COALESCE(p.NAME,'Produto indisponível'),COALESCE(u.SYMBOL,''),i.QUANTITY,i.UNIT_PRICE FROM SALE_ITEM i LEFT JOIN PRODUCT p ON p.ID=i.PRODUCT_ID LEFT JOIN UNIT u ON u.ID=p.UNIT_ID WHERE i.SALE_ID=? AND i.STATUS=1 ORDER BY i.ID",(rs,i)->{
            var quantity=rs.getBigDecimal(3);var price=rs.getBigDecimal(4);
            return new Item(rs.getString(1),rs.getString(2),quantity,price,price==null?null:quantity.multiply(price).setScale(2,RoundingMode.HALF_UP));
        },id);
        BigDecimal adjustment = items.stream().anyMatch(item -> item.subtotal()==null) ? BigDecimal.ZERO
            : sale.total().subtract(items.stream().map(Item::subtotal).reduce(BigDecimal.ZERO,BigDecimal::add));
        return new Receipt(sale.id(),sale.date(),sale.client(),sale.operator(),sale.total(),adjustment,items);
    }
}

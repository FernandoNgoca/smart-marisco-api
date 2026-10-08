package mz.com.sgp.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class DashboardOverviewService {
    private final JdbcTemplate jdbc;
    public DashboardOverviewService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Totals(long sales, BigDecimal revenue) {}
    public record Day(String name, long value, BigDecimal revenue) {}
    public record Pending(long count, BigDecimal value, Long oldestDays) {}
    public record Product(Long id, String name, String unit, BigDecimal quantity) {}
    public record TopProduct(String name, String unit, BigDecimal quantity, String image) {}
    public record Overview(String from, String to, String previousFrom, String previousTo, String updatedAt,
        Totals totals, Totals previous, BigDecimal averageSale, Pending pending, long restockCount,
        List<Product> restock, List<Day> dailySales, List<TopProduct> topProducts) {}
    private static final String SALES = " FROM SALE s WHERE s.STATUS=1 AND s.SALE_STATUS='COMPLETED' AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)>=? AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)<?";

    public Overview overview(LocalDate from, LocalDate to) { return overviewAt(from, to, LocalDateTime.now()); }
    Overview overviewAt(LocalDate from, LocalDate to, LocalDateTime now) {
        if (from == null) from = now.toLocalDate().withDayOfMonth(1);
        if (to == null) to = now.toLocalDate();
        long days = ChronoUnit.DAYS.between(from, to) + 1;
        if (days < 1 || days > 366 || to.isAfter(now.toLocalDate()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Escolha um período de até 366 dias, sem datas futuras.");
        var start = from.atStartOfDay(); var end = to.plusDays(1).atStartOfDay();
        var previousStart = start.minusDays(days); var previousEnd = start;
        var totals = totals(start, end); var previous = totals(previousStart, previousEnd);
        var pending = jdbc.queryForObject("SELECT COUNT(*), COALESCE(SUM(TOTAL_VALUE),0), MIN(CREATED_DATE) FROM SALE WHERE STATUS=1 AND SALE_STATUS='ORDERS'", (rs,i) -> {
            var oldest = rs.getTimestamp(3);
            return new Pending(rs.getLong(1), rs.getBigDecimal(2), oldest == null ? null : Math.max(0, ChronoUnit.DAYS.between(oldest.toLocalDateTime().toLocalDate(), now.toLocalDate())));
        });
        String stock = " FROM PRODUCT p LEFT JOIN STOCK st ON st.PRODUCT_ID=p.ID AND st.STATUS=1 LEFT JOIN UNIT u ON u.ID=p.UNIT_ID WHERE p.STATUS=1 GROUP BY p.ID,p.NAME,u.SYMBOL HAVING COALESCE(SUM(st.QUANTITY),0)<=5";
        long restockCount = jdbc.queryForObject("SELECT COUNT(*) FROM (SELECT p.ID" + stock + ") low_stock", Long.class);
        var restock = jdbc.query("SELECT p.ID,p.NAME,COALESCE(u.SYMBOL,''),COALESCE(SUM(st.QUANTITY),0) quantity" + stock + " ORDER BY quantity,p.NAME,p.ID LIMIT 5",
            (rs,i) -> new Product(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getBigDecimal(4)));
        Map<LocalDate,Day> counts = new HashMap<>();
        jdbc.query("SELECT CAST(COALESCE(s.COMPLETED_DATE,s.CREATED_DATE) AS DATE),COUNT(*),COALESCE(SUM(s.TOTAL_VALUE),0)" + SALES + " GROUP BY CAST(COALESCE(s.COMPLETED_DATE,s.CREATED_DATE) AS DATE)",
            (org.springframework.jdbc.core.RowCallbackHandler) rs -> counts.put(rs.getDate(1).toLocalDate(),new Day(rs.getDate(1).toLocalDate().toString(),rs.getLong(2),rs.getBigDecimal(3))), start,end);
        List<Day> daily = new ArrayList<>();
        for(var date=from; !date.isAfter(to); date=date.plusDays(1)) daily.add(counts.getOrDefault(date,new Day(date.toString(),0L,BigDecimal.ZERO)));
        var top = jdbc.query("SELECT ranked.NAME,ranked.unit,ranked.quantity,photo.IMAGE FROM (SELECT p.ID,p.NAME,COALESCE(u.SYMBOL,'') unit,SUM(i.QUANTITY) quantity FROM SALE_ITEM i JOIN SALE s ON s.ID=i.SALE_ID JOIN PRODUCT p ON p.ID=i.PRODUCT_ID LEFT JOIN UNIT u ON u.ID=p.UNIT_ID WHERE i.STATUS=1 AND s.STATUS=1 AND s.SALE_STATUS='COMPLETED' AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)>=? AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)<? GROUP BY p.ID,p.NAME,u.SYMBOL ORDER BY quantity DESC,p.ID LIMIT 5) ranked JOIN PRODUCT photo ON photo.ID=ranked.ID ORDER BY ranked.quantity DESC,ranked.ID",
            (rs,i) -> new TopProduct(rs.getString(1),rs.getString(2),rs.getBigDecimal(3),rs.getString(4)),start,end);
        return new Overview(from.toString(),to.toString(),previousStart.toLocalDate().toString(),previousEnd.toLocalDate().minusDays(1).toString(),now.toString(),totals,previous,
            totals.sales()==0 ? BigDecimal.ZERO : totals.revenue().divide(BigDecimal.valueOf(totals.sales()),2,RoundingMode.HALF_UP),pending,restockCount,restock,daily,top);
    }
    private Totals totals(LocalDateTime start,LocalDateTime end) {
        return jdbc.queryForObject("SELECT COUNT(*),COALESCE(SUM(s.TOTAL_VALUE),0)"+SALES,(rs,i)->new Totals(rs.getLong(1),rs.getBigDecimal(2)),start,end);
    }
}

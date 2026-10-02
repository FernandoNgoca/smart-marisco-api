package mz.com.sgp.reports;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
@Transactional(readOnly = true)
public class ReportService {
    private final JdbcTemplate jdbc;
    public ReportService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public record Column(String label, String kind) {}
    public record Metric(String label, BigDecimal value, String kind) {}
    public record Report(String title, String period, String generatedAt, List<Column> columns,
        List<List<Object>> rows, long totalElements, List<Metric> metrics) {}
    record Query(String title, String select, String from, String order, List<Column> columns, List<Object> args) {}
    private Column col(String label, String kind) { return new Column(label, kind); }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }

    public Report read(String type, LocalDate from, LocalDate to, String search, String filter, String seller, int page, int size, boolean export) {
        if (page < 0 || size < 1 || size > (export ? 10000 : 100)) throw bad("Paginação inválida");
        if (from == null) from = LocalDate.now().withDayOfMonth(1);
        if (to == null) to = LocalDate.now();
        if (from.isAfter(to) || to.getYear() > 9998) throw bad("Período inválido");
        search = search == null ? "" : search.trim();
        seller = seller == null ? "" : seller.trim();
        filter = filter == null ? "" : filter;
        if (search.length() > 100 || seller.length() > 100) throw bad("Filtro demasiado longo");
        List<Object> args = new ArrayList<>();
        String where;
        Query q;
        String like = "%" + search.toLowerCase(Locale.ROOT).replace("!", "!!").replace("%", "!%").replace("_", "!_") + "%";
        switch (type) {
            case "sales" -> {
                where = " FROM SALE s LEFT JOIN CLIENT c ON c.ID=s.CLIENT_ID WHERE s.STATUS=1 AND s.SALE_STATUS='COMPLETED' AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)>=? AND COALESCE(s.COMPLETED_DATE,s.CREATED_DATE)<?";
                args.add(from.atStartOfDay()); args.add(to.plusDays(1).atStartOfDay());
                if (!seller.isEmpty()) { where += " AND s.CREATED_BY=?"; args.add(seller); }
                q = new Query("Vendas", "s.ID, COALESCE(s.COMPLETED_DATE,s.CREATED_DATE), COALESCE(CONCAT(c.FIRST_NAME, ' ', COALESCE(c.LAST_NAME,'')), 'Consumidor final'), COALESCE(s.CREATED_BY,'Não registado'), s.TOTAL_VALUE", where,
                    "COALESCE(s.COMPLETED_DATE,s.CREATED_DATE) DESC, s.ID DESC", List.of(col("Venda", "text"), col("Data de conclusão", "date"), col("Cliente", "text"), col("Registado por", "text"), col("Total (MZN)", "money")), args);
            }
            case "stock" -> {
                where = " FROM STOCK s JOIN PRODUCT p ON p.ID=s.PRODUCT_ID LEFT JOIN UNIT u ON u.ID=p.UNIT_ID WHERE s.STATUS=1 AND p.STATUS=1";
                if (!search.isEmpty()) { where += " AND (LOWER(p.NAME) LIKE ? ESCAPE '!' OR LOWER(p.CODE) LIKE ? ESCAPE '!')"; args.add(like); args.add(like); }
                if (filter.equals("low")) where += " AND s.QUANTITY>0 AND s.QUANTITY<=5";
                else if (filter.equals("empty")) where += " AND s.QUANTITY<=0";
                else if (!filter.isEmpty()) throw bad("Estado de stock inválido");
                q = new Query("Stock atual", "p.CODE, p.NAME, s.QUANTITY, COALESCE(u.SYMBOL,''), CASE WHEN s.QUANTITY<=0 THEN 'Esgotado' WHEN s.QUANTITY<=5 THEN 'Stock baixo' ELSE 'Disponível' END", where, "p.NAME, s.ID", List.of(col("Código", "text"), col("Produto", "text"), col("Quantidade", "number"), col("Unidade", "text"), col("Estado", "text")), args);
            }
            case "movements" -> {
                where = " FROM STOCK_MOVEMENT m JOIN STOCK s ON s.ID=m.STOCK_ID JOIN PRODUCT p ON p.ID=s.PRODUCT_ID LEFT JOIN UNIT u ON u.ID=p.UNIT_ID WHERE m.STATUS=1 AND m.CREATED_DATE>=? AND m.CREATED_DATE<?";
                args.add(from.atStartOfDay()); args.add(to.plusDays(1).atStartOfDay());
                if (!search.isEmpty()) { where += " AND (LOWER(p.NAME) LIKE ? ESCAPE '!' OR LOWER(p.CODE) LIKE ? ESCAPE '!')"; args.add(like); args.add(like); }
                if (filter.equals("ENTRY") || filter.equals("EXIT")) { where += " AND m.MOVEMENT_TYPE=?"; args.add(filter); }
                else if (!filter.isEmpty()) throw bad("Tipo de movimento inválido");
                q = new Query("Movimentos de stock", "m.CREATED_DATE, p.CODE, p.NAME, CASE WHEN m.MOVEMENT_TYPE='ENTRY' THEN 'Entrada' ELSE 'Saída' END, CASE WHEN m.MOVEMENT_TYPE='ENTRY' THEN m.QUANTITY ELSE -m.QUANTITY END, COALESCE(u.SYMBOL,''), COALESCE(m.DESCRIPTION,'Não registada'), COALESCE(m.CREATED_BY,'Não registado')", where, "m.CREATED_DATE DESC, m.ID DESC", List.of(col("Data", "date"),col("Código", "text"),col("Produto", "text"),col("Tipo", "text"),col("Quantidade", "number"),col("Unidade", "text"),col("Origem / referência", "text"),col("Responsável", "text")), args);
            }
            default -> throw bad("Relatório inválido");
        }
        long count = jdbc.queryForObject("SELECT COUNT(*)" + q.from(), Long.class, args.toArray());
        if (export && count > 10000) throw bad("O relatório ultrapassa 10 000 linhas. Reduza os filtros para exportar ou imprimir.");
        List<Metric> metrics = new ArrayList<>();
        metrics.add(new Metric(type.equals("sales") ? "Vendas concluídas" : "Registos", BigDecimal.valueOf(count), "number"));
        if (type.equals("sales")) {
            BigDecimal revenue = jdbc.queryForObject("SELECT COALESCE(SUM(s.TOTAL_VALUE),0)" + q.from(), BigDecimal.class, args.toArray());
            metrics.add(new Metric("Faturação", revenue, "money"));
            metrics.add(new Metric("Valor médio por venda", count == 0 ? BigDecimal.ZERO : revenue.divide(BigDecimal.valueOf(count), 2, java.math.RoundingMode.HALF_UP), "money"));
        }
        if (type.equals("stock")) {
            for (String[] status : new String[][]{{"Esgotados", "s.QUANTITY<=0"}, {"Stock baixo", "s.QUANTITY>0 AND s.QUANTITY<=5"}}) {
                long value = jdbc.queryForObject("SELECT COUNT(*)" + q.from() + " AND " + status[1], Long.class, args.toArray());
                metrics.add(new Metric(status[0], BigDecimal.valueOf(value), "number"));
            }
        }
        List<Object> pagedArgs = new ArrayList<>(args); pagedArgs.add(size); pagedArgs.add((long)page*size);
        List<List<Object>> rows = jdbc.query("SELECT " + q.select() + q.from() + " ORDER BY " + q.order() + " LIMIT ? OFFSET ?", (rs, index) -> {
            List<Object> row = new ArrayList<>();
            for (int i=1;i<=q.columns().size();i++) {
                Object value = rs.getObject(i);
                if (value instanceof java.sql.Timestamp date) value = date.toLocalDateTime().toString();
                if (value instanceof java.time.LocalDateTime date) value = date.toString();
                row.add(value == null ? "" : value);
            }
            return row;
        }, pagedArgs.toArray());
        String period = type.equals("stock") ? "Posição atual · stock baixo: até 5 unidades de medida" : from + " a " + to;
        if (!search.isEmpty() && !type.equals("sales")) period += " · Produto: " + search;
        if (!seller.isEmpty() && type.equals("sales")) period += " · Registado por: " + seller;
        if (!filter.isEmpty() && !type.equals("sales")) period += " · " + switch(filter) { case "low" -> "Stock baixo"; case "empty" -> "Esgotado"; case "ENTRY" -> "Entradas"; default -> "Saídas"; };
        return new Report(q.title(), period, java.time.LocalDateTime.now().toString(), q.columns(), rows, count, metrics);
    }
}

package mz.com.sgp.services;
import java.time.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class DashboardOverviewTests {
    private DashboardOverviewService service() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
        jdbc.execute("CREATE TABLE SALE(ID BIGINT,STATUS INT,SALE_STATUS VARCHAR,CREATED_DATE TIMESTAMP,COMPLETED_DATE TIMESTAMP,TOTAL_VALUE DECIMAL(12,2))");
        jdbc.execute("CREATE TABLE PRODUCT(ID BIGINT,NAME VARCHAR,STATUS INT,UNIT_ID BIGINT)");
        jdbc.execute("CREATE TABLE UNIT(ID BIGINT,SYMBOL VARCHAR)");
        jdbc.execute("CREATE TABLE STOCK(ID BIGINT,PRODUCT_ID BIGINT,STATUS INT,QUANTITY DECIMAL(10,3))");
        jdbc.execute("CREATE TABLE SALE_ITEM(ID BIGINT,SALE_ID BIGINT,PRODUCT_ID BIGINT,STATUS INT,QUANTITY DECIMAL(10,3))");
        jdbc.update("INSERT INTO UNIT VALUES(1,'kg')");
        jdbc.update("INSERT INTO PRODUCT VALUES(1,'Camarão',1,1),(2,'Sem stock',1,1),(3,'Inativo',0,1),(4,'Peixe',1,1)");
        jdbc.update("INSERT INTO STOCK VALUES(1,1,1,3),(2,3,1,0),(3,4,1,10)");
        jdbc.update("INSERT INTO SALE VALUES(1,1,'COMPLETED','2026-09-01 12:00:00','2026-10-02 10:00:00',100),(2,1,'COMPLETED','2026-09-30 12:00:00',NULL,40),(3,1,'ORDERS','2026-09-20 12:00:00',NULL,300),(4,1,'CANCELED','2026-09-01 12:00:00',NULL,900),(5,0,'ORDERS','2026-08-01 12:00:00',NULL,1000)");
        jdbc.update("INSERT INTO SALE_ITEM VALUES(1,1,1,1,2),(2,2,4,1,1),(3,4,4,1,50)");
        return new DashboardOverviewService(jdbc);
    }
    private void check(boolean value){if(!value)throw new AssertionError();}
    @Test void respectsDatesAndKeepsCurrentAttentionIndependentOfPeriod(){
        var service=service();var now=LocalDateTime.of(2026,10,2,12,0);
        var data=service.overviewAt(LocalDate.of(2026,10,1),LocalDate.of(2026,10,2),now);
        check(data.totals().sales()==1 && data.totals().revenue().intValueExact()==100);
        check(data.previous().sales()==1 && data.previous().revenue().intValueExact()==40);
        check(data.previousFrom().equals("2026-09-29") && data.previousTo().equals("2026-09-30"));
        check(data.dailySales().size()==2 && data.dailySales().get(0).value()==0 && data.dailySales().get(1).value()==1);
        check(data.pending().count()==1 && data.pending().value().intValueExact()==300 && data.pending().oldestDays()==12);
        check(data.restockCount()==2 && data.restock().get(0).quantity().signum()==0);
        check(data.restock().get(1).unit().equals("kg"));
        check(data.topProducts().size()==1 && data.topProducts().get(0).name().equals("Camarão"));
        var past=service.overviewAt(LocalDate.of(2026,9,29),LocalDate.of(2026,9,30),now);
        check(past.pending().equals(data.pending()) && past.restock().equals(data.restock()));
        var empty=service.overviewAt(LocalDate.of(2026,9,1),LocalDate.of(2026,9,2),now);
        check(empty.totals().sales()==0 && empty.averageSale().signum()==0);
    }
    @Test void rejectsInvalidRanges(){
        var service=service();var now=LocalDateTime.of(2026,10,2,12,0);
        for(var dates:new LocalDate[][]{{LocalDate.of(2026,10,2),LocalDate.of(2026,10,1)},{LocalDate.of(2026,10,1),LocalDate.of(2026,10,3)},{LocalDate.of(2025,1,1),LocalDate.of(2026,10,2)}}){
            try{service.overviewAt(dates[0],dates[1],now);throw new AssertionError();}catch(org.springframework.web.server.ResponseStatusException expected){check(expected.getStatusCode().value()==400);}
        }
    }
    public static void main(String[] args){var tests=new DashboardOverviewTests();tests.respectsDatesAndKeepsCurrentAttentionIndependentOfPeriod();tests.rejectsInvalidRanges();System.out.println("Dashboard checks passed: periods, comparisons, pending orders, restock, empty days, units, range validation.");}
}

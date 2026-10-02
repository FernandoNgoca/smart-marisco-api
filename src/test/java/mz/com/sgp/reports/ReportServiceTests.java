package mz.com.sgp.reports;

import java.time.LocalDate;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

class ReportServiceTests {
    private ReportService service() {
        var jdbc = new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:" + java.util.UUID.randomUUID() + ";MODE=MySQL;DB_CLOSE_DELAY=-1", "sa", ""));
        jdbc.execute("CREATE TABLE CLIENT(ID BIGINT, FIRST_NAME VARCHAR, LAST_NAME VARCHAR)");
        jdbc.execute("CREATE TABLE SALE(ID BIGINT, CLIENT_ID BIGINT, STATUS INT, SALE_STATUS VARCHAR, COMPLETED_DATE TIMESTAMP, CREATED_DATE TIMESTAMP, CREATED_BY VARCHAR, TOTAL_VALUE DECIMAL(12,2))");
        jdbc.execute("CREATE TABLE UNIT(ID BIGINT, SYMBOL VARCHAR)");
        jdbc.execute("CREATE TABLE PRODUCT(ID BIGINT, UNIT_ID BIGINT, CODE VARCHAR, NAME VARCHAR, STATUS INT)");
        jdbc.execute("CREATE TABLE STOCK(ID BIGINT, PRODUCT_ID BIGINT, STATUS INT, QUANTITY DECIMAL(10,3))");
        jdbc.execute("CREATE TABLE STOCK_MOVEMENT(ID BIGINT, STOCK_ID BIGINT, STATUS INT, CREATED_DATE TIMESTAMP, MOVEMENT_TYPE VARCHAR, QUANTITY DECIMAL(10,3), DESCRIPTION VARCHAR, CREATED_BY VARCHAR)");
        jdbc.update("INSERT INTO CLIENT VALUES(1,'=SUM(A1:A9)','<Cliente>')");
        for (int i=1;i<=15;i++) jdbc.update("INSERT INTO SALE VALUES(?,1,1,'COMPLETED','2026-09-30 23:59:59','2026-08-01 12:00:00','seller',10)",i);
        jdbc.update("INSERT INTO SALE VALUES(16,NULL,1,'ORDERS',NULL,'2026-09-30 12:00:00','seller',500)");
        jdbc.update("INSERT INTO SALE VALUES(17,NULL,1,'COMPLETED','2026-10-01 00:00:00','2026-09-30 12:00:00','seller',500)");
        jdbc.update("INSERT INTO SALE VALUES(18,NULL,0,'COMPLETED','2026-09-30 12:00:00','2026-09-30 12:00:00','seller',500)");
        jdbc.update("INSERT INTO UNIT VALUES(1,'kg')");
        jdbc.update("INSERT INTO PRODUCT VALUES(1,1,'P001','Camarão',1),(2,1,'P002','Peixe',1),(3,1,'P003','Inativo',0)");
        jdbc.update("INSERT INTO STOCK VALUES(1,1,1,3.500),(2,2,1,0),(3,3,1,2)");
        jdbc.update("INSERT INTO STOCK_MOVEMENT VALUES(1,1,1,'2026-09-30 08:00:00','ENTRY',5,'Entrada manual','seller'),(2,1,1,'2026-09-30 09:00:00','EXIT',1.5,'Venda #1','seller')");
        return new ReportService(jdbc);
    }
    private ReportService.Report read(ReportService service, String type, String search, String filter, int page) {
        return service.read(type,LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),search,filter,"",page,10,false);
    }
    private void check(boolean condition) { if(!condition) throw new AssertionError(); }
    @Test void salesUseCompletionDateAndFullFilteredTotals() {
        var service=service(); var result=read(service,"sales","","",0);
        check(result.totalElements()==15 && result.rows().size()==10);
        check(result.metrics().get(1).value().intValueExact()==150);
        check(result.metrics().get(2).value().intValueExact()==10);
        check(read(service,"sales","","",1).rows().size()==5);
        check(service.read("sales",LocalDate.of(2026,9,1),LocalDate.of(2026,9,30),"","","other",0,10,false).totalElements()==0);
    }
    @Test void stockAndMovementsRespectFiltersAndUnits() {
        var service=service();
        check(read(service,"stock","","",0).totalElements()==2);
        check(read(service,"stock","","low",0).totalElements()==1);
        check(read(service,"stock","","empty",0).totalElements()==1);
        check(read(service,"stock","%","",0).totalElements()==0);
        check(read(service,"movements","Camarão","EXIT",0).rows().get(0).get(4).toString().equals("-1.500"));
    }
    @Test void workbookStoresTextWithoutExecutingFormulas() throws Exception {
        var result=read(service(),"sales","","",0);
        try(var zip=new ZipInputStream(new ByteArrayInputStream(ReportWorkbook.write(result)))) {
            String sheet="";int entries=0;
            for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()) { entries++;if(entry.getName().endsWith("sheet1.xml")) sheet=new String(zip.readAllBytes(),StandardCharsets.UTF_8); }
            check(entries==5);check(sheet.contains("=SUM(A1:A9) &lt;Cliente&gt;"));check(!sheet.contains("<f>"));check(sheet.contains("<v>150.00</v>"));
        }
    }
    @Test void reportAuthorizationOnlyAllowsNonAdministrativeManagers() {
        String rule = ReportController.class.getAnnotation(org.springframework.security.access.prepost.PreAuthorize.class).value();
        var expression = new org.springframework.expression.spel.standard.SpelExpressionParser().parseExpression(rule);
        for (String[] roles : new String[][]{{"ROLE_MANAGER"}, {"ROLE_USER"}, {"ROLE_ADMIN"}, {"ROLE_ADMIN", "ROLE_MANAGER"}}) {
            var auth = new org.springframework.security.authentication.TestingAuthenticationToken("user", "", roles);
            var root = new org.springframework.security.access.expression.SecurityExpressionRoot(auth) {};
            boolean allowed = Boolean.TRUE.equals(expression.getValue(new org.springframework.expression.spel.support.StandardEvaluationContext(root), Boolean.class));
            check(allowed == (roles.length == 1 && roles[0].equals("ROLE_MANAGER")));
        }
    }
    public static void main(String[] args) throws Exception {
        var tests=new ReportServiceTests(); tests.salesUseCompletionDateAndFullFilteredTotals(); tests.stockAndMovementsRespectFiltersAndUnits(); tests.workbookStoresTextWithoutExecutingFormulas(); tests.reportAuthorizationOnlyAllowsNonAdministrativeManagers();
        System.out.println("Report checks passed: completion dates, totals, pagination, stock filters, literal search, units, XLSX escaping.");
    }
}

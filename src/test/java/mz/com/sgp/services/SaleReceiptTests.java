package mz.com.sgp.services;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
class SaleReceiptTests {
 @Test void receiptUsesSavedPricesAndRejectsPendingSales(){
  var jdbc=new JdbcTemplate(new DriverManagerDataSource("jdbc:h2:mem:"+java.util.UUID.randomUUID()+";MODE=MySQL;DB_CLOSE_DELAY=-1","sa",""));
  jdbc.execute("CREATE TABLE SALE(ID BIGINT,CLIENT_ID BIGINT,STATUS INT,SALE_STATUS VARCHAR,CREATED_DATE TIMESTAMP,COMPLETED_DATE TIMESTAMP,CREATED_BY VARCHAR,TOTAL_VALUE DECIMAL(12,2))");
  jdbc.execute("CREATE TABLE CLIENT(ID BIGINT,FIRST_NAME VARCHAR,LAST_NAME VARCHAR)");
  jdbc.execute("CREATE TABLE PRODUCT(ID BIGINT,NAME VARCHAR,UNIT_ID BIGINT,SALE_PRICE DECIMAL(12,2))");
  jdbc.execute("CREATE TABLE UNIT(ID BIGINT,SYMBOL VARCHAR)");
  jdbc.execute("CREATE TABLE SALE_ITEM(ID BIGINT,SALE_ID BIGINT,PRODUCT_ID BIGINT,STATUS INT,QUANTITY DECIMAL(10,3),UNIT_PRICE DECIMAL(12,2))");
  jdbc.update("INSERT INTO SALE VALUES(1,NULL,1,'COMPLETED','2026-10-02 10:00:00',NULL,'seller',15),(2,NULL,1,'ORDERS','2026-10-02 10:00:00',NULL,'seller',15),(3,NULL,1,'COMPLETED','2026-10-02 10:00:00',NULL,'seller',20)");
  jdbc.update("INSERT INTO UNIT VALUES(1,'kg')");jdbc.update("INSERT INTO PRODUCT VALUES(1,'Camarão',1,999)");
  jdbc.update("INSERT INTO SALE_ITEM VALUES(1,1,1,1,1.5,10),(2,3,1,1,2,NULL)");
  var service=new SaleReceiptService(jdbc);var receipt=service.receipt(1L);
  check(receipt.client().equals("Consumidor final"));check(receipt.total().intValueExact()==15);
  check(receipt.items().get(0).unitPrice().intValueExact()==10 && receipt.items().get(0).subtotal().intValueExact()==15);
  check(receipt.roundingAdjustment().signum()==0);
  check(service.receipt(3L).items().get(0).unitPrice()==null);
  try{service.receipt(2L);throw new AssertionError();}catch(org.springframework.web.server.ResponseStatusException e){check(e.getStatusCode().value()==409);}
  try{service.receipt(99L);throw new AssertionError();}catch(org.springframework.web.server.ResponseStatusException e){check(e.getStatusCode().value()==404);}
 }
 private void check(boolean value){if(!value)throw new AssertionError();}
 public static void main(String[] args){new SaleReceiptTests().receiptUsesSavedPricesAndRejectsPendingSales();System.out.println("Receipt checks passed: saved prices, totals, consumer sale, legacy prices, pending/missing sale.");}
}

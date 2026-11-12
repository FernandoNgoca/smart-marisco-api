package mz.com.sgp.data.dto;

import java.math.BigDecimal;

/** Resumo dos registos filtrados no histórico: quantos são e quanto foi faturado (só concluídos). */
public record SaleSummaryDTO(long count, BigDecimal completedTotal) {
}

package mz.com.sgp.data.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;

public record DashboardSummaryDTO(BigDecimal revenueToday, BigDecimal revenueMonth,
        long salesToday, long salesYesterday, long salesMonth, long salesPreviousPeriod,
        long lowStockProducts, BigDecimal lowStockThreshold,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime updatedAt,
        @JsonSerialize(using = LocalDateTimeSerializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime previousPeriodEnd) { }

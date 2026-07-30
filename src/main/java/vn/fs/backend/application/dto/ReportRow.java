package vn.fs.backend.application.dto;

import java.math.BigDecimal;

public record ReportRow(
        String group,
        long quantity,
        BigDecimal revenue,
        BigDecimal averagePrice,
        BigDecimal minimumPrice,
        BigDecimal maximumPrice) {
}

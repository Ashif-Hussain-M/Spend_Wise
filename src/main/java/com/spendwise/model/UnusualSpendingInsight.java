package com.spendwise.model;

import java.math.BigDecimal;

public record UnusualSpendingInsight(
        Long categoryId,
        String categoryName,
        BigDecimal currentSpending,
        BigDecimal baselineSpending,
        double percentDifference,
        BigDecimal absoluteIncrease,
        String reason
) {
}

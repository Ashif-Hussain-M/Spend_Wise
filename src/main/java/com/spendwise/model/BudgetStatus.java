package com.spendwise.model;

import java.math.BigDecimal;

public record BudgetStatus(
        Long budgetId,
        Long categoryId,
        String categoryName,
        int month,
        int year,
        BigDecimal budgetAmount,
        BigDecimal actualSpent,
        BigDecimal remainingAmount,
        double utilizationPercentage,
        String statusLabel
) {
}

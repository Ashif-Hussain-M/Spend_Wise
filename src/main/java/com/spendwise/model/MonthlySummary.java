package com.spendwise.model;

import java.math.BigDecimal;

public record MonthlySummary(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        BigDecimal netSavings,
        BigDecimal savingsRate
) {
}

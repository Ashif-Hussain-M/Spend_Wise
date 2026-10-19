package com.spendwise.model;

import java.math.BigDecimal;

public record SpendingForecast(
        int year,
        int month,
        int elapsedDays,
        int totalDaysInMonth,
        BigDecimal monthToDateSpending,
        BigDecimal projectedTotalSpending,
        BigDecimal budgetAmount,
        BigDecimal projectedVariance,
        boolean isPastMonth,
        String explanation
) {
}

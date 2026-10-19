package com.spendwise.model;

import java.math.BigDecimal;

public record MonthComparison(
        int year,
        int month,
        BigDecimal income,
        BigDecimal expense,
        BigDecimal netSavings
) {
}

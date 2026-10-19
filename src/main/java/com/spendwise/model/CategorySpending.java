package com.spendwise.model;

import java.math.BigDecimal;

public record CategorySpending(
        Long categoryId,
        String categoryName,
        CategoryType categoryType,
        BigDecimal totalAmount,
        int transactionCount,
        double percentageOfTotal
) {
}

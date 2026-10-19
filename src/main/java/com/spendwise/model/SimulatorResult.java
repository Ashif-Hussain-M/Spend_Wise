package com.spendwise.model;

import java.math.BigDecimal;
import java.util.Map;

public record SimulatorResult(
        BigDecimal originalTotalExpense,
        BigDecimal totalPotentialSavings,
        BigDecimal projectedNewExpense,
        Map<Long, BigDecimal> categorySavingsMap
) {
}

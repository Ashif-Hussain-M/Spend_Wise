package com.spendwise.repository;

import com.spendwise.model.BudgetStatus;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ReportRepository {

    MonthlySummary getMonthlySummary(Long userId, int year, int month);

    List<CategorySpending> getCategorySpending(Long userId, int year, int month, CategoryType type);

    List<MonthComparison> getMonthlyHistory(Long userId, int numberOfMonths, LocalDate referenceDate);

    List<BudgetStatus> getBudgetStatuses(Long userId, int year, int month);

    Optional<BudgetStatus> getOverallBudgetStatus(Long userId, int year, int month);

    BigDecimal getCategorySpendingInDateRange(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate);

    BigDecimal getCategorySpendingInElapsedDayWindow(Long userId, Long categoryId, int year, int month, int maxDayOfMonth);

    boolean hasDataInMonth(Long userId, int year, int month);
}

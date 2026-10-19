package com.spendwise.service.fakes;

import com.spendwise.model.BudgetStatus;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.repository.ReportRepository;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public class FakeReportRepository implements ReportRepository {

    private boolean failWithConnectionError = false;
    private MonthlySummary mockSummary = new MonthlySummary(MoneyUtil.zero(), MoneyUtil.zero(), MoneyUtil.zero(), MoneyUtil.zero());
    private final List<CategorySpending> mockSpending = new ArrayList<>();
    private final List<MonthComparison> mockHistory = new ArrayList<>();
    private final List<BudgetStatus> mockBudgetStatuses = new ArrayList<>();
    private BudgetStatus mockOverallBudgetStatus = null;
    private final Map<String, BigDecimal> elapsedDaySpending = new HashMap<>();
    private final Set<String> monthsWithData = new HashSet<>();

    public void setFailWithConnectionError(boolean fail) {
        this.failWithConnectionError = fail;
    }

    public void setMockSummary(MonthlySummary summary) {
        this.mockSummary = summary;
    }

    public void addCategorySpending(CategorySpending spending) {
        this.mockSpending.add(spending);
    }

    public void addMonthComparison(MonthComparison mc) {
        this.mockHistory.add(mc);
    }

    public void addBudgetStatus(BudgetStatus bs) {
        this.mockBudgetStatuses.add(bs);
    }

    public void setMockOverallBudgetStatus(BudgetStatus obs) {
        this.mockOverallBudgetStatus = obs;
    }

    public void setElapsedDaySpending(Long categoryId, int year, int month, int day, BigDecimal amount) {
        elapsedDaySpending.put(categoryId + ":" + year + ":" + month + ":" + day, amount);
    }

    public void markMonthHasData(int year, int month) {
        monthsWithData.add(year + ":" + month);
    }

    @Override
    public MonthlySummary getMonthlySummary(Long userId, int year, int month) {
        checkFailure();
        return mockSummary;
    }

    @Override
    public List<CategorySpending> getCategorySpending(Long userId, int year, int month, CategoryType type) {
        checkFailure();
        return mockSpending;
    }

    @Override
    public List<MonthComparison> getMonthlyHistory(Long userId, int numberOfMonths, LocalDate referenceDate) {
        checkFailure();
        return mockHistory;
    }

    @Override
    public List<BudgetStatus> getBudgetStatuses(Long userId, int year, int month) {
        checkFailure();
        return mockBudgetStatuses;
    }

    @Override
    public Optional<BudgetStatus> getOverallBudgetStatus(Long userId, int year, int month) {
        checkFailure();
        return Optional.ofNullable(mockOverallBudgetStatus);
    }

    @Override
    public BigDecimal getCategorySpendingInDateRange(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        checkFailure();
        return mockSummary.totalExpense();
    }

    @Override
    public BigDecimal getCategorySpendingInElapsedDayWindow(Long userId, Long categoryId, int year, int month, int maxDayOfMonth) {
        checkFailure();
        return elapsedDaySpending.getOrDefault(categoryId + ":" + year + ":" + month + ":" + maxDayOfMonth, MoneyUtil.zero());
    }

    @Override
    public boolean hasDataInMonth(Long userId, int year, int month) {
        checkFailure();
        return monthsWithData.contains(year + ":" + month);
    }

    private void checkFailure() {
        if (failWithConnectionError) {
            throw new RuntimeException("Communications link failure: Connection refused");
        }
    }
}

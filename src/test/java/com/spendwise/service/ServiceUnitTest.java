package com.spendwise.service;

import com.spendwise.model.Budget;
import com.spendwise.model.BudgetStatus;
import com.spendwise.model.Category;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.SimulatorResult;
import com.spendwise.model.SpendingForecast;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.model.UnusualSpendingInsight;
import com.spendwise.service.fakes.FakeBudgetRepository;
import com.spendwise.service.fakes.FakeCategoryRepository;
import com.spendwise.service.fakes.FakeReportRepository;
import com.spendwise.service.fakes.FakeTransactionRepository;
import com.spendwise.util.MoneyUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ServiceUnitTest {

    private FakeCategoryRepository categoryRepo;
    private FakeTransactionRepository transactionRepo;
    private FakeBudgetRepository budgetRepo;
    private FakeReportRepository reportRepo;

    private CategoryService categoryService;
    private TransactionService transactionService;
    private BudgetService budgetService;
    private ReportService reportService;
    private SpendingAnalysisService analysisService;

    @BeforeEach
    void setUp() {
        categoryRepo = new FakeCategoryRepository();
        transactionRepo = new FakeTransactionRepository();
        budgetRepo = new FakeBudgetRepository();
        reportRepo = new FakeReportRepository();

        categoryService = new CategoryService(categoryRepo);
        transactionService = new TransactionService(transactionRepo, categoryRepo);
        budgetService = new BudgetService(budgetRepo, categoryRepo);
        reportService = new ReportService(reportRepo);
        analysisService = new SpendingAnalysisService(reportRepo, categoryRepo);
    }

    @Test
    void testValidIncomeAndExpenseAddEditDelete() {
        Category salaryCat = categoryService.createCategory(1L, "Salary", CategoryType.INCOME);
        Category rentCat = categoryService.createCategory(1L, "Rent", CategoryType.EXPENSE);

        Transaction income = transactionService.addTransaction(
                1L, salaryCat.getCategoryId(), TransactionType.INCOME,
                new BigDecimal("95000.00"), LocalDate.of(2026, 10, 1), "Salary Credit"
        );
        assertNotNull(income.getTransactionId());
        assertEquals(new BigDecimal("95000.00"), income.getAmount());

        Transaction expense = transactionService.addTransaction(
                1L, rentCat.getCategoryId(), TransactionType.EXPENSE,
                new BigDecimal("25000.00"), LocalDate.of(2026, 10, 2), "Flat Rent"
        );
        assertNotNull(expense.getTransactionId());
        assertEquals(new BigDecimal("25000.00"), expense.getAmount());

        boolean updated = transactionService.updateTransaction(
                1L, expense.getTransactionId(), rentCat.getCategoryId(), TransactionType.EXPENSE,
                new BigDecimal("26000.00"), LocalDate.of(2026, 10, 2), "Rent Updated"
        );
        assertTrue(updated);
        assertEquals(new BigDecimal("26000.00"), transactionService.getTransactionById(expense.getTransactionId()).get().getAmount());

        boolean deleted = transactionService.deleteTransaction(expense.getTransactionId());
        assertTrue(deleted);
        assertFalse(transactionService.getTransactionById(expense.getTransactionId()).isPresent());
    }

    @Test
    void testInvalidAmountThrowsValidationException() {
        Category rentCat = categoryService.createCategory(1L, "Rent", CategoryType.EXPENSE);

        assertThrows(ValidationException.class, () -> transactionService.addTransaction(
                1L, rentCat.getCategoryId(), TransactionType.EXPENSE,
                BigDecimal.ZERO, LocalDate.of(2026, 10, 2), "Zero amount"
        ));

        assertThrows(ValidationException.class, () -> transactionService.addTransaction(
                1L, rentCat.getCategoryId(), TransactionType.EXPENSE,
                new BigDecimal("-50.00"), LocalDate.of(2026, 10, 2), "Negative amount"
        ));

        assertThrows(ValidationException.class, () -> transactionService.addTransaction(
                1L, rentCat.getCategoryId(), TransactionType.EXPENSE,
                null, LocalDate.of(2026, 10, 2), "Null amount"
        ));
    }

    @Test
    void testInvalidCategoryThrowsValidationException() {
        Category salaryCat = categoryService.createCategory(1L, "Salary", CategoryType.INCOME);

        assertThrows(ValidationException.class, () -> transactionService.addTransaction(
                1L, 9999L, TransactionType.EXPENSE,
                new BigDecimal("500.00"), LocalDate.of(2026, 10, 2), "Nonexistent category"
        ));

        assertThrows(ValidationException.class, () -> transactionService.addTransaction(
                1L, salaryCat.getCategoryId(), TransactionType.EXPENSE,
                new BigDecimal("500.00"), LocalDate.of(2026, 10, 2), "Type mismatch"
        ));
    }

    @Test
    void testMonthlyTotalsAndNetSavings() {
        reportRepo.setMockSummary(new MonthlySummary(
                new BigDecimal("95000.00"),
                new BigDecimal("35000.00"),
                new BigDecimal("60000.00"),
                new BigDecimal("63.16")
        ));

        MonthlySummary summary = reportService.getMonthlySummary(1L, 2026, 10);
        assertEquals(new BigDecimal("95000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("35000.00"), summary.totalExpense());
        assertEquals(new BigDecimal("60000.00"), summary.netSavings());
        assertEquals(new BigDecimal("63.16"), summary.savingsRate());
    }

    @Test
    void testBudgetUtilizationAndThresholdWarnings() {
        reportRepo.setMockOverallBudgetStatus(new BudgetStatus(
                1L, null, "Overall Budget", 10, 2026,
                new BigDecimal("50000.00"), new BigDecimal("42000.00"),
                new BigDecimal("8000.00"), 84.0, "Near limit"
        ));

        Optional<BudgetStatus> status = reportService.getOverallBudgetStatus(1L, 2026, 10);
        assertTrue(status.isPresent());
        assertEquals(84.0, status.get().utilizationPercentage());
        assertEquals("Near limit", status.get().statusLabel());

        reportService.setWarningThresholdPercent(90.0);
        Optional<BudgetStatus> adjustedStatus = reportService.getOverallBudgetStatus(1L, 2026, 10);
        assertTrue(adjustedStatus.isPresent());
        assertEquals("On track", adjustedStatus.get().statusLabel());
    }

    @Test
    void testDuplicateBudgetUpsert() {
        Category rentCat = categoryService.createCategory(1L, "Rent", CategoryType.EXPENSE);

        Budget b1 = budgetService.setCategoryBudget(1L, rentCat.getCategoryId(), 2026, 10, new BigDecimal("25000.00"));
        assertEquals(new BigDecimal("25000.00"), b1.getBudgetAmount());

        Budget b2 = budgetService.setCategoryBudget(1L, rentCat.getCategoryId(), 2026, 10, new BigDecimal("28000.00"));
        assertEquals(b1.getBudgetId(), b2.getBudgetId());
        assertEquals(new BigDecimal("28000.00"), b2.getBudgetAmount());

        List<Budget> budgets = budgetService.getBudgetsForMonth(1L, 2026, 10);
        assertEquals(1, budgets.size());
    }

    @Test
    void testEmptyAndInsufficientHistoryForUnusualSpending() {
        Category groceries = categoryService.createCategory(1L, "Groceries", CategoryType.EXPENSE);

        List<UnusualSpendingInsight> emptyResult = analysisService.detectUnusualSpending(1L, LocalDate.of(2026, 10, 15));
        assertTrue(emptyResult.isEmpty());

        reportRepo.markMonthHasData(2026, 9);
        List<UnusualSpendingInsight> singleMonthResult = analysisService.detectUnusualSpending(1L, LocalDate.of(2026, 10, 15));
        assertTrue(singleMonthResult.isEmpty());

        reportRepo.markMonthHasData(2026, 8);
        reportRepo.setElapsedDaySpending(groceries.getCategoryId(), 2026, 8, 15, new BigDecimal("3000.00"));
        reportRepo.setElapsedDaySpending(groceries.getCategoryId(), 2026, 9, 15, new BigDecimal("3000.00"));
        reportRepo.setElapsedDaySpending(groceries.getCategoryId(), 2026, 10, 15, new BigDecimal("6500.00"));

        List<UnusualSpendingInsight> flagged = analysisService.detectUnusualSpending(1L, LocalDate.of(2026, 10, 15));
        assertEquals(1, flagged.size());
        assertEquals("Groceries", flagged.get(0).categoryName());
        assertTrue(flagged.get(0).percentDifference() >= 100.0);
        assertTrue(flagged.get(0).reason().contains("Potential unusual spending pattern"));
    }

    @Test
    void testForecastDayOneAndLeapYear() {
        reportRepo.setMockSummary(new MonthlySummary(
                MoneyUtil.zero(), new BigDecimal("1000.00"), MoneyUtil.zero(), MoneyUtil.zero()
        ));

        SpendingForecast dayOneForecast = analysisService.forecastMonthlySpending(1L, 2026, 10, LocalDate.of(2026, 10, 1));
        assertEquals(1, dayOneForecast.elapsedDays());
        assertEquals(31, dayOneForecast.totalDaysInMonth());
        assertEquals(new BigDecimal("31000.00"), dayOneForecast.projectedTotalSpending());
        assertFalse(dayOneForecast.isPastMonth());

        reportRepo.setMockSummary(new MonthlySummary(
                MoneyUtil.zero(), new BigDecimal("2900.00"), MoneyUtil.zero(), MoneyUtil.zero()
        ));
        SpendingForecast leapYearForecast = analysisService.forecastMonthlySpending(1L, 2024, 2, LocalDate.of(2024, 2, 10));
        assertEquals(29, leapYearForecast.totalDaysInMonth());
        assertEquals(10, leapYearForecast.elapsedDays());
        assertEquals(new BigDecimal("8410.00"), leapYearForecast.projectedTotalSpending());

        reportRepo.setMockSummary(new MonthlySummary(
                MoneyUtil.zero(), new BigDecimal("2800.00"), MoneyUtil.zero(), MoneyUtil.zero()
        ));
        SpendingForecast nonLeapYearForecast = analysisService.forecastMonthlySpending(1L, 2025, 2, LocalDate.of(2025, 2, 10));
        assertEquals(28, nonLeapYearForecast.totalDaysInMonth());
        assertEquals(new BigDecimal("7840.00"), nonLeapYearForecast.projectedTotalSpending());
    }

    @Test
    void testSimulatorLeavesDataUnchanged() {
        Map<Long, BigDecimal> currentSpendings = Map.of(
                1L, new BigDecimal("10000.00"),
                2L, new BigDecimal("6000.00")
        );
        Map<Long, Double> reductions = Map.of(
                1L, 20.0,
                2L, 50.0
        );

        BigDecimal originalTotal = new BigDecimal("16000.00");
        SimulatorResult result = analysisService.simulateSavings(originalTotal, currentSpendings, reductions);

        assertEquals(new BigDecimal("16000.00"), result.originalTotalExpense());
        assertEquals(new BigDecimal("5000.00"), result.totalPotentialSavings());
        assertEquals(new BigDecimal("11000.00"), result.projectedNewExpense());
        assertEquals(new BigDecimal("2000.00"), result.categorySavingsMap().get(1L));
        assertEquals(new BigDecimal("3000.00"), result.categorySavingsMap().get(2L));

        assertEquals(0, transactionRepo.findByUserId(1L).size());
        assertEquals(0, budgetRepo.findAllByUserId(1L).size());
    }

    @Test
    void testConnectionFailureHandled() {
        reportRepo.setFailWithConnectionError(true);
        assertThrows(DataAccessException.class, () -> reportService.getMonthlySummary(1L, 2026, 10));
    }
}

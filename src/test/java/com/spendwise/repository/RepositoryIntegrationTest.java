package com.spendwise.repository;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.model.Budget;
import com.spendwise.model.BudgetStatus;
import com.spendwise.model.Category;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.util.MoneyUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Statement;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RepositoryIntegrationTest {

    private static CategoryRepository categoryRepo;
    private static TransactionRepository transactionRepo;
    private static BudgetRepository budgetRepo;
    private static ReportRepository reportRepo;

    @BeforeAll
    static void setUpAll() {
        DatabaseConnection.setTestMode(true);
        categoryRepo = new JdbcCategoryRepository();
        transactionRepo = new JdbcTransactionRepository();
        budgetRepo = new JdbcBudgetRepository();
        reportRepo = new JdbcReportRepository();
    }

    @AfterAll
    static void tearDownAll() {
        DatabaseConnection.setTestMode(false);
    }

    @BeforeEach
    void cleanDatabase() throws Exception {
        try (Connection conn = DatabaseConnection.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("DELETE FROM transactions");
            stmt.execute("DELETE FROM budgets");
            stmt.execute("DELETE FROM categories");
            stmt.execute("DELETE FROM users");
            stmt.execute("INSERT INTO users (user_id, username) VALUES (1, 'default_user')");
        }
    }

    @Test
    void testCategoryCrudAndCount() {
        Category cat = new Category();
        cat.setUserId(1L);
        cat.setCategoryName("Groceries");
        cat.setCategoryType(CategoryType.EXPENSE);

        Category saved = categoryRepo.save(cat);
        assertNotNull(saved.getCategoryId());

        Optional<Category> found = categoryRepo.findById(saved.getCategoryId());
        assertTrue(found.isPresent());
        assertEquals("Groceries", found.get().getCategoryName());

        saved.setCategoryName("Daily Groceries");
        boolean updated = categoryRepo.update(saved);
        assertTrue(updated);

        Optional<Category> updatedFound = categoryRepo.findByNameAndType(1L, "daily groceries", CategoryType.EXPENSE);
        assertTrue(updatedFound.isPresent());
        assertEquals("Daily Groceries", updatedFound.get().getCategoryName());

        assertEquals(0, categoryRepo.countTransactions(saved.getCategoryId()));

        boolean deleted = categoryRepo.deleteById(saved.getCategoryId());
        assertTrue(deleted);
        assertFalse(categoryRepo.findById(saved.getCategoryId()).isPresent());
    }

    @Test
    void testTransactionCrudAndFilters() {
        Category cat = categoryRepo.save(new Category(null, 1L, "Rent", CategoryType.EXPENSE, null));

        Transaction tx = new Transaction();
        tx.setUserId(1L);
        tx.setCategoryId(cat.getCategoryId());
        tx.setTransactionType(TransactionType.EXPENSE);
        tx.setAmount(new BigDecimal("25000.00"));
        tx.setTransactionDate(LocalDate.of(2026, 10, 2));
        tx.setDescription("October Indiranagar Rent");

        Transaction saved = transactionRepo.save(tx);
        assertNotNull(saved.getTransactionId());

        Optional<Transaction> found = transactionRepo.findById(saved.getTransactionId());
        assertTrue(found.isPresent());
        assertEquals(new BigDecimal("25000.00"), found.get().getAmount());
        assertEquals("Rent", found.get().getCategoryName());

        List<Transaction> filtered = transactionRepo.findFiltered(
                1L,
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 31),
                cat.getCategoryId(),
                TransactionType.EXPENSE,
                new BigDecimal("20000.00"),
                new BigDecimal("30000.00"),
                "indiranagar"
        );
        assertEquals(1, filtered.size());

        List<Transaction> unmatched = transactionRepo.findFiltered(
                1L, null, null, null, null, null, null, "nonexistent"
        );
        assertTrue(unmatched.isEmpty());

        saved.setDescription("Updated Rent");
        saved.setAmount(new BigDecimal("26000.00"));
        boolean updated = transactionRepo.update(saved);
        assertTrue(updated);

        assertEquals(1, categoryRepo.countTransactions(cat.getCategoryId()));

        boolean deleted = transactionRepo.deleteById(saved.getTransactionId());
        assertTrue(deleted);
        assertEquals(0, categoryRepo.countTransactions(cat.getCategoryId()));
    }

    @Test
    void testBudgetCrud() {
        Category cat = categoryRepo.save(new Category(null, 1L, "Dining", CategoryType.EXPENSE, null));

        Budget overall = new Budget();
        overall.setUserId(1L);
        overall.setCategoryId(null);
        overall.setBudgetMonth(10);
        overall.setBudgetYear(2026);
        overall.setBudgetAmount(new BigDecimal("70000.00"));

        Budget savedOverall = budgetRepo.save(overall);
        assertNotNull(savedOverall.getBudgetId());

        Budget catBudget = new Budget();
        catBudget.setUserId(1L);
        catBudget.setCategoryId(cat.getCategoryId());
        catBudget.setBudgetMonth(10);
        catBudget.setBudgetYear(2026);
        catBudget.setBudgetAmount(new BigDecimal("6000.00"));

        Budget savedCatBudget = budgetRepo.save(catBudget);
        assertNotNull(savedCatBudget.getBudgetId());

        Optional<Budget> foundOverall = budgetRepo.findOverallBudget(1L, 2026, 10);
        assertTrue(foundOverall.isPresent());
        assertEquals(new BigDecimal("70000.00"), foundOverall.get().getBudgetAmount());

        Optional<Budget> foundCatBudget = budgetRepo.findCategoryBudget(1L, cat.getCategoryId(), 2026, 10);
        assertTrue(foundCatBudget.isPresent());
        assertEquals("Dining", foundCatBudget.get().getCategoryName());

        List<Budget> monthlyBudgets = budgetRepo.findByMonth(1L, 2026, 10);
        assertEquals(2, monthlyBudgets.size());

        savedOverall.setBudgetAmount(new BigDecimal("75000.00"));
        boolean updated = budgetRepo.update(savedOverall);
        assertTrue(updated);

        boolean deleted = budgetRepo.deleteById(savedOverall.getBudgetId());
        assertTrue(deleted);
        assertFalse(budgetRepo.findOverallBudget(1L, 2026, 10).isPresent());
    }

    @Test
    void testReportAggregation() {
        Category salary = categoryRepo.save(new Category(null, 1L, "Salary", CategoryType.INCOME, null));
        Category groceries = categoryRepo.save(new Category(null, 1L, "Groceries", CategoryType.EXPENSE, null));
        Category dining = categoryRepo.save(new Category(null, 1L, "Dining", CategoryType.EXPENSE, null));

        transactionRepo.save(new Transaction(null, 1L, salary.getCategoryId(), null,
                TransactionType.INCOME, new BigDecimal("100000.00"), LocalDate.of(2026, 10, 1), "Salary", null));
        transactionRepo.save(new Transaction(null, 1L, groceries.getCategoryId(), null,
                TransactionType.EXPENSE, new BigDecimal("8000.00"), LocalDate.of(2026, 10, 5), "Groceries", null));
        transactionRepo.save(new Transaction(null, 1L, dining.getCategoryId(), null,
                TransactionType.EXPENSE, new BigDecimal("4000.00"), LocalDate.of(2026, 10, 10), "Dinner", null));

        budgetRepo.save(new Budget(null, 1L, null, null, 10, 2026, new BigDecimal("50000.00"), null, null));
        budgetRepo.save(new Budget(null, 1L, groceries.getCategoryId(), null, 10, 2026, new BigDecimal("10000.00"), null, null));

        MonthlySummary summary = reportRepo.getMonthlySummary(1L, 2026, 10);
        assertEquals(new BigDecimal("100000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("12000.00"), summary.totalExpense());
        assertEquals(new BigDecimal("88000.00"), summary.netSavings());
        assertEquals(new BigDecimal("88.00"), summary.savingsRate());

        List<CategorySpending> spending = reportRepo.getCategorySpending(1L, 2026, 10, CategoryType.EXPENSE);
        assertEquals(2, spending.size());
        assertEquals("Groceries", spending.get(0).categoryName());
        assertEquals(new BigDecimal("8000.00"), spending.get(0).totalAmount());
        assertTrue(spending.get(0).percentageOfTotal() > 60.0);

        List<BudgetStatus> statuses = reportRepo.getBudgetStatuses(1L, 2026, 10);
        assertEquals(1, statuses.size());
        assertEquals("Groceries", statuses.get(0).categoryName());
        assertEquals(new BigDecimal("10000.00"), statuses.get(0).budgetAmount());
        assertEquals(new BigDecimal("8000.00"), statuses.get(0).actualSpent());
        assertEquals(new BigDecimal("2000.00"), statuses.get(0).remainingAmount());
        assertEquals(80.0, statuses.get(0).utilizationPercentage(), 0.01);
        assertEquals("Near limit", statuses.get(0).statusLabel());

        Optional<BudgetStatus> overallStatus = reportRepo.getOverallBudgetStatus(1L, 2026, 10);
        assertTrue(overallStatus.isPresent());
        assertEquals(new BigDecimal("50000.00"), overallStatus.get().budgetAmount());
        assertEquals(new BigDecimal("12000.00"), overallStatus.get().actualSpent());
        assertEquals(new BigDecimal("38000.00"), overallStatus.get().remainingAmount());
        assertEquals("On track", overallStatus.get().statusLabel());

        List<MonthComparison> history = reportRepo.getMonthlyHistory(1L, 3, LocalDate.of(2026, 10, 15));
        assertEquals(1, history.size());
        assertEquals(2026, history.get(0).year());
        assertEquals(10, history.get(0).month());

        assertTrue(reportRepo.hasDataInMonth(1L, 2026, 10));
        assertFalse(reportRepo.hasDataInMonth(1L, 2026, 11));

        BigDecimal windowSpent = reportRepo.getCategorySpendingInElapsedDayWindow(1L, groceries.getCategoryId(), 2026, 10, 7);
        assertEquals(new BigDecimal("8000.00"), windowSpent);
        BigDecimal earlyWindowSpent = reportRepo.getCategorySpendingInElapsedDayWindow(1L, groceries.getCategoryId(), 2026, 10, 4);
        assertEquals(new BigDecimal("0.00"), earlyWindowSpent);
    }
}

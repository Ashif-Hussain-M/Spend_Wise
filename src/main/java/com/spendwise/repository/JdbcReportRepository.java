package com.spendwise.repository;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.model.BudgetStatus;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcReportRepository implements ReportRepository {

    @Override
    public MonthlySummary getMonthlySummary(Long userId, int year, int month) {
        String sql = "SELECT " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'INCOME' THEN amount ELSE 0 END), 0) AS total_income, " +
                "COALESCE(SUM(CASE WHEN transaction_type = 'EXPENSE' THEN amount ELSE 0 END), 0) AS total_expense " +
                "FROM transactions " +
                "WHERE user_id = ? AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal income = MoneyUtil.of(rs.getBigDecimal("total_income"));
                    BigDecimal expense = MoneyUtil.of(rs.getBigDecimal("total_expense"));
                    BigDecimal savings = MoneyUtil.of(income.subtract(expense));
                    BigDecimal savingsRate = MoneyUtil.zero();
                    if (income.compareTo(BigDecimal.ZERO) > 0) {
                        savingsRate = savings.divide(income, 4, RoundingMode.HALF_UP)
                                .multiply(BigDecimal.valueOf(100))
                                .setScale(2, RoundingMode.HALF_UP);
                    }
                    return new MonthlySummary(income, expense, savings, savingsRate);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing monthly summary: " + e.getMessage(), e);
        }
        return new MonthlySummary(MoneyUtil.zero(), MoneyUtil.zero(), MoneyUtil.zero(), MoneyUtil.zero());
    }

    @Override
    public List<CategorySpending> getCategorySpending(Long userId, int year, int month, CategoryType type) {
        String sql = "SELECT c.category_id, c.category_name, c.category_type, " +
                "COALESCE(SUM(t.amount), 0) AS total_amount, " +
                "COUNT(t.transaction_id) AS tx_count " +
                "FROM categories c " +
                "JOIN transactions t ON c.category_id = t.category_id " +
                "WHERE c.user_id = ? " +
                "AND YEAR(t.transaction_date) = ? " +
                "AND MONTH(t.transaction_date) = ? " +
                "AND c.category_type = ? " +
                "GROUP BY c.category_id, c.category_name, c.category_type " +
                "HAVING total_amount > 0 " +
                "ORDER BY total_amount DESC";

        List<CategorySpending> list = new ArrayList<>();
        BigDecimal grandTotal = BigDecimal.ZERO;

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setString(4, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal amount = MoneyUtil.of(rs.getBigDecimal("total_amount"));
                    grandTotal = grandTotal.add(amount);
                    list.add(new CategorySpending(
                            rs.getLong("category_id"),
                            rs.getString("category_name"),
                            CategoryType.valueOf(rs.getString("category_type")),
                            amount,
                            rs.getInt("tx_count"),
                            0.0
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing category spending: " + e.getMessage(), e);
        }

        if (grandTotal.compareTo(BigDecimal.ZERO) > 0) {
            List<CategorySpending> withPercentages = new ArrayList<>(list.size());
            for (CategorySpending cs : list) {
                double pct = cs.totalAmount().divide(grandTotal, 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100)).doubleValue();
                withPercentages.add(new CategorySpending(
                        cs.categoryId(),
                        cs.categoryName(),
                        cs.categoryType(),
                        cs.totalAmount(),
                        cs.transactionCount(),
                        pct
                ));
            }
            return withPercentages;
        }

        return list;
    }

    @Override
    public List<MonthComparison> getMonthlyHistory(Long userId, int numberOfMonths, LocalDate referenceDate) {
        LocalDate startDate = referenceDate.minusMonths(numberOfMonths - 1).withDayOfMonth(1);
        LocalDate endDate = referenceDate.withDayOfMonth(referenceDate.lengthOfMonth());

        String sql = "SELECT YEAR(t.transaction_date) AS yr, MONTH(t.transaction_date) AS mo, " +
                "COALESCE(SUM(CASE WHEN t.transaction_type = 'INCOME' THEN t.amount ELSE 0 END), 0) AS total_income, " +
                "COALESCE(SUM(CASE WHEN t.transaction_type = 'EXPENSE' THEN t.amount ELSE 0 END), 0) AS total_expense " +
                "FROM transactions t " +
                "WHERE t.user_id = ? AND t.transaction_date >= ? AND t.transaction_date <= ? " +
                "GROUP BY YEAR(t.transaction_date), MONTH(t.transaction_date) " +
                "ORDER BY yr ASC, mo ASC";

        List<MonthComparison> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setDate(2, Date.valueOf(startDate));
            ps.setDate(3, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    BigDecimal income = MoneyUtil.of(rs.getBigDecimal("total_income"));
                    BigDecimal expense = MoneyUtil.of(rs.getBigDecimal("total_expense"));
                    BigDecimal netSavings = MoneyUtil.of(income.subtract(expense));
                    list.add(new MonthComparison(
                            rs.getInt("yr"),
                            rs.getInt("mo"),
                            income,
                            expense,
                            netSavings
                    ));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing monthly history: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<BudgetStatus> getBudgetStatuses(Long userId, int year, int month) {
        String sql = "SELECT b.budget_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, " +
                "COALESCE(actual.total_spent, 0) AS actual_spent " +
                "FROM budgets b " +
                "JOIN categories c ON b.category_id = c.category_id " +
                "LEFT JOIN (" +
                "    SELECT category_id, SUM(amount) AS total_spent " +
                "    FROM transactions " +
                "    WHERE user_id = ? " +
                "      AND YEAR(transaction_date) = ? " +
                "      AND MONTH(transaction_date) = ? " +
                "      AND transaction_type = 'EXPENSE' " +
                "    GROUP BY category_id" +
                ") actual ON b.category_id = actual.category_id " +
                "WHERE b.user_id = ? AND b.budget_year = ? AND b.budget_month = ? AND b.category_id IS NOT NULL " +
                "ORDER BY c.category_name ASC";

        List<BudgetStatus> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setLong(4, userId);
            ps.setInt(5, year);
            ps.setInt(6, month);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(buildBudgetStatus(rs, rs.getString("category_name")));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing budget statuses: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Optional<BudgetStatus> getOverallBudgetStatus(Long userId, int year, int month) {
        String sql = "SELECT b.budget_id, b.category_id, " +
                "b.budget_month, b.budget_year, b.budget_amount, " +
                "(" +
                "    SELECT COALESCE(SUM(amount), 0) " +
                "    FROM transactions " +
                "    WHERE user_id = ? " +
                "      AND YEAR(transaction_date) = ? " +
                "      AND MONTH(transaction_date) = ? " +
                "      AND transaction_type = 'EXPENSE'" +
                ") AS actual_spent " +
                "FROM budgets b " +
                "WHERE b.user_id = ? AND b.budget_year = ? AND b.budget_month = ? AND b.category_id IS NULL";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            ps.setLong(4, userId);
            ps.setInt(5, year);
            ps.setInt(6, month);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(buildBudgetStatus(rs, "Overall Budget"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing overall budget status: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public BigDecimal getCategorySpendingInDateRange(Long userId, Long categoryId, LocalDate startDate, LocalDate endDate) {
        boolean hasCategory = (categoryId != null);
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total_spent " +
                "FROM transactions " +
                "WHERE user_id = ? " + (hasCategory ? "AND category_id = ? " : "") +
                "AND transaction_date >= ? AND transaction_date <= ? " +
                "AND transaction_type = 'EXPENSE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setLong(idx++, userId);
            if (hasCategory) {
                ps.setLong(idx++, categoryId);
            }
            ps.setDate(idx++, Date.valueOf(startDate));
            ps.setDate(idx++, Date.valueOf(endDate));
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return MoneyUtil.of(rs.getBigDecimal("total_spent"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing spending in date range: " + e.getMessage(), e);
        }
        return MoneyUtil.zero();
    }

    @Override
    public BigDecimal getCategorySpendingInElapsedDayWindow(Long userId, Long categoryId, int year, int month, int maxDayOfMonth) {
        boolean hasCategory = (categoryId != null);
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total_spent " +
                "FROM transactions " +
                "WHERE user_id = ? " + (hasCategory ? "AND category_id = ? " : "") +
                "AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? " +
                "AND DAY(transaction_date) <= ? AND transaction_type = 'EXPENSE'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            int idx = 1;
            ps.setLong(idx++, userId);
            if (hasCategory) {
                ps.setLong(idx++, categoryId);
            }
            ps.setInt(idx++, year);
            ps.setInt(idx++, month);
            ps.setInt(idx++, maxDayOfMonth);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return MoneyUtil.of(rs.getBigDecimal("total_spent"));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error computing elapsed day spending: " + e.getMessage(), e);
        }
        return MoneyUtil.zero();
    }

    @Override
    public boolean hasDataInMonth(Long userId, int year, int month) {
        String sql = "SELECT 1 FROM transactions " +
                "WHERE user_id = ? AND YEAR(transaction_date) = ? AND MONTH(transaction_date) = ? " +
                "LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error checking monthly data existence: " + e.getMessage(), e);
        }
    }

    private BudgetStatus buildBudgetStatus(ResultSet rs, String categoryName) throws SQLException {
        Long budgetId = rs.getLong("budget_id");
        Long categoryId = rs.getLong("category_id");
        if (rs.wasNull()) {
            categoryId = null;
        }
        int month = rs.getInt("budget_month");
        int year = rs.getInt("budget_year");
        BigDecimal budgetAmount = MoneyUtil.of(rs.getBigDecimal("budget_amount"));
        BigDecimal actualSpent = MoneyUtil.of(rs.getBigDecimal("actual_spent"));
        BigDecimal remaining = MoneyUtil.of(budgetAmount.subtract(actualSpent));

        double pct = 0.0;
        if (budgetAmount.compareTo(BigDecimal.ZERO) > 0) {
            pct = actualSpent.divide(budgetAmount, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100)).doubleValue();
        }

        String label;
        if (pct > 100.0) {
            label = "Over budget";
        } else if (pct >= 80.0) {
            label = "Near limit";
        } else {
            label = "On track";
        }

        return new BudgetStatus(budgetId, categoryId, categoryName, month, year,
                budgetAmount, actualSpent, remaining, pct, label);
    }
}

package com.spendwise.repository;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.model.Budget;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcBudgetRepository implements BudgetRepository {

    @Override
    public Optional<Budget> findById(Long budgetId) {
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, b.category_key, b.created_at " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.budget_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, budgetId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding budget by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Budget> findOverallBudget(Long userId, int year, int month) {
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, b.category_key, b.created_at " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.budget_year = ? AND b.budget_month = ? AND b.category_id IS NULL";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding overall budget: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Budget> findCategoryBudget(Long userId, Long categoryId, int year, int month) {
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, b.category_key, b.created_at " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.category_id = ? AND b.budget_year = ? AND b.budget_month = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setLong(2, categoryId);
            ps.setInt(3, year);
            ps.setInt(4, month);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding category budget: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Budget> findByMonth(Long userId, int year, int month) {
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, b.category_key, b.created_at " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? AND b.budget_year = ? AND b.budget_month = ? " +
                "ORDER BY (b.category_id IS NOT NULL), c.category_name ASC";
        List<Budget> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, year);
            ps.setInt(3, month);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding budgets by month: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Budget> findAllByUserId(Long userId) {
        String sql = "SELECT b.budget_id, b.user_id, b.category_id, c.category_name, " +
                "b.budget_month, b.budget_year, b.budget_amount, b.category_key, b.created_at " +
                "FROM budgets b " +
                "LEFT JOIN categories c ON b.category_id = c.category_id " +
                "WHERE b.user_id = ? " +
                "ORDER BY b.budget_year DESC, b.budget_month DESC, (b.category_id IS NOT NULL), c.category_name ASC";
        List<Budget> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding all budgets: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Budget save(Budget budget) {
        String sql = "INSERT INTO budgets (user_id, category_id, budget_month, budget_year, budget_amount) " +
                "VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, budget.getUserId());
            if (budget.getCategoryId() == null) {
                ps.setNull(2, Types.BIGINT);
            } else {
                ps.setLong(2, budget.getCategoryId());
            }
            ps.setInt(3, budget.getBudgetMonth());
            ps.setInt(4, budget.getBudgetYear());
            ps.setBigDecimal(5, budget.getBudgetAmount());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    budget.setBudgetId(keys.getLong(1));
                }
            }
            return budget;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving budget: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Budget budget) {
        String sql = "UPDATE budgets SET budget_amount = ? WHERE budget_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBigDecimal(1, budget.getBudgetAmount());
            ps.setLong(2, budget.getBudgetId());
            ps.setLong(3, budget.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating budget: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long budgetId) {
        String sql = "DELETE FROM budgets WHERE budget_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, budgetId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting budget: " + e.getMessage(), e);
        }
    }

    private Budget mapRow(ResultSet rs) throws SQLException {
        Budget b = new Budget();
        b.setBudgetId(rs.getLong("budget_id"));
        b.setUserId(rs.getLong("user_id"));
        long catId = rs.getLong("category_id");
        if (!rs.wasNull()) {
            b.setCategoryId(catId);
        }
        b.setCategoryName(rs.getString("category_name"));
        b.setBudgetMonth(rs.getInt("budget_month"));
        b.setBudgetYear(rs.getInt("budget_year"));
        b.setBudgetAmount(rs.getBigDecimal("budget_amount"));
        b.setCategoryKey(rs.getLong("category_key"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            b.setCreatedAt(ts.toLocalDateTime());
        }
        return b;
    }
}

package com.spendwise.repository;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcTransactionRepository implements TransactionRepository {

    @Override
    public Optional<Transaction> findById(Long transactionId) {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, " +
                "t.transaction_type, t.amount, t.transaction_date, t.description, t.created_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transaction by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Transaction> findByUserId(Long userId) {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, " +
                "t.transaction_type, t.amount, t.transaction_date, t.description, t.created_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding transactions by user: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Transaction> findFiltered(Long userId,
                                          LocalDate startDate,
                                          LocalDate endDate,
                                          Long categoryId,
                                          TransactionType type,
                                          BigDecimal minAmount,
                                          BigDecimal maxAmount,
                                          String searchDescription) {
        StringBuilder sql = new StringBuilder(
                "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, " +
                "t.transaction_type, t.amount, t.transaction_date, t.description, t.created_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? "
        );
        List<Object> params = new ArrayList<>();
        params.add(userId);

        if (startDate != null) {
            sql.append("AND t.transaction_date >= ? ");
            params.add(Date.valueOf(startDate));
        }
        if (endDate != null) {
            sql.append("AND t.transaction_date <= ? ");
            params.add(Date.valueOf(endDate));
        }
        if (categoryId != null) {
            sql.append("AND t.category_id = ? ");
            params.add(categoryId);
        }
        if (type != null) {
            sql.append("AND t.transaction_type = ? ");
            params.add(type.name());
        }
        if (minAmount != null) {
            sql.append("AND t.amount >= ? ");
            params.add(minAmount);
        }
        if (maxAmount != null) {
            sql.append("AND t.amount <= ? ");
            params.add(maxAmount);
        }
        if (searchDescription != null && !searchDescription.trim().isEmpty()) {
            sql.append("AND LOWER(t.description) LIKE ? ");
            params.add("%" + searchDescription.trim().toLowerCase() + "%");
        }

        sql.append("ORDER BY t.transaction_date DESC, t.transaction_id DESC");

        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error searching transactions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Transaction> findRecent(Long userId, int limit) {
        String sql = "SELECT t.transaction_id, t.user_id, t.category_id, c.category_name, " +
                "t.transaction_type, t.amount, t.transaction_date, t.description, t.created_at " +
                "FROM transactions t " +
                "JOIN categories c ON t.category_id = c.category_id " +
                "WHERE t.user_id = ? " +
                "ORDER BY t.transaction_date DESC, t.transaction_id DESC " +
                "LIMIT ?";
        List<Transaction> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding recent transactions: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Transaction save(Transaction transaction) {
        String sql = "INSERT INTO transactions (user_id, category_id, transaction_type, amount, transaction_date, description) " +
                "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, transaction.getUserId());
            ps.setLong(2, transaction.getCategoryId());
            ps.setString(3, transaction.getTransactionType().name());
            ps.setBigDecimal(4, transaction.getAmount());
            ps.setDate(5, Date.valueOf(transaction.getTransactionDate()));
            ps.setString(6, transaction.getDescription().trim());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    transaction.setTransactionId(keys.getLong(1));
                }
            }
            return transaction;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Transaction transaction) {
        String sql = "UPDATE transactions SET category_id = ?, transaction_type = ?, amount = ?, " +
                "transaction_date = ?, description = ? WHERE transaction_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transaction.getCategoryId());
            ps.setString(2, transaction.getTransactionType().name());
            ps.setBigDecimal(3, transaction.getAmount());
            ps.setDate(4, Date.valueOf(transaction.getTransactionDate()));
            ps.setString(5, transaction.getDescription().trim());
            ps.setLong(6, transaction.getTransactionId());
            ps.setLong(7, transaction.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating transaction: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long transactionId) {
        String sql = "DELETE FROM transactions WHERE transaction_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, transactionId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting transaction: " + e.getMessage(), e);
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Transaction tx = new Transaction();
        tx.setTransactionId(rs.getLong("transaction_id"));
        tx.setUserId(rs.getLong("user_id"));
        tx.setCategoryId(rs.getLong("category_id"));
        tx.setCategoryName(rs.getString("category_name"));
        tx.setTransactionType(TransactionType.valueOf(rs.getString("transaction_type")));
        tx.setAmount(rs.getBigDecimal("amount"));
        Date d = rs.getDate("transaction_date");
        if (d != null) {
            tx.setTransactionDate(d.toLocalDate());
        }
        tx.setDescription(rs.getString("description"));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            tx.setCreatedAt(ts.toLocalDateTime());
        }
        return tx;
    }
}

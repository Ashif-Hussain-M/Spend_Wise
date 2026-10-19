package com.spendwise.repository;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class JdbcCategoryRepository implements CategoryRepository {

    @Override
    public Optional<Category> findById(Long categoryId) {
        String sql = "SELECT c.category_id, c.user_id, c.category_name, c.category_type, c.created_at, " +
                "(SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id) AS tx_count " +
                "FROM categories c WHERE c.category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding category by id: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public Optional<Category> findByNameAndType(Long userId, String name, CategoryType type) {
        String sql = "SELECT c.category_id, c.user_id, c.category_name, c.category_type, c.created_at, " +
                "(SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id) AS tx_count " +
                "FROM categories c WHERE c.user_id = ? AND LOWER(c.category_name) = LOWER(?) AND c.category_type = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, name.trim());
            ps.setString(3, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding category by name and type: " + e.getMessage(), e);
        }
        return Optional.empty();
    }

    @Override
    public List<Category> findByUserId(Long userId) {
        String sql = "SELECT c.category_id, c.user_id, c.category_name, c.category_type, c.created_at, " +
                "(SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id) AS tx_count " +
                "FROM categories c WHERE c.user_id = ? ORDER BY c.category_type ASC, c.category_name ASC";
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding categories for user: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public List<Category> findByUserIdAndType(Long userId, CategoryType type) {
        String sql = "SELECT c.category_id, c.user_id, c.category_name, c.category_type, c.created_at, " +
                "(SELECT COUNT(*) FROM transactions t WHERE t.category_id = c.category_id) AS tx_count " +
                "FROM categories c WHERE c.user_id = ? AND c.category_type = ? ORDER BY c.category_name ASC";
        List<Category> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, userId);
            ps.setString(2, type.name());
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error finding categories by type: " + e.getMessage(), e);
        }
        return list;
    }

    @Override
    public Category save(Category category) {
        String sql = "INSERT INTO categories (user_id, category_name, category_type) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, category.getUserId());
            ps.setString(2, category.getCategoryName().trim());
            ps.setString(3, category.getCategoryType().name());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    category.setCategoryId(keys.getLong(1));
                }
            }
            return category;
        } catch (SQLException e) {
            throw new RuntimeException("Error saving category: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean update(Category category) {
        String sql = "UPDATE categories SET category_name = ?, category_type = ? WHERE category_id = ? AND user_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category.getCategoryName().trim());
            ps.setString(2, category.getCategoryType().name());
            ps.setLong(3, category.getCategoryId());
            ps.setLong(4, category.getUserId());
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error updating category: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteById(Long categoryId) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("Error deleting category: " + e.getMessage(), e);
        }
    }

    @Override
    public int countTransactions(Long categoryId) {
        String sql = "SELECT COUNT(*) FROM transactions WHERE category_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, categoryId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Error counting transactions for category: " + e.getMessage(), e);
        }
        return 0;
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        Category category = new Category();
        category.setCategoryId(rs.getLong("category_id"));
        category.setUserId(rs.getLong("user_id"));
        category.setCategoryName(rs.getString("category_name"));
        category.setCategoryType(CategoryType.valueOf(rs.getString("category_type")));
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) {
            category.setCreatedAt(ts.toLocalDateTime());
        }
        category.setTransactionCount(rs.getInt("tx_count"));
        return category;
    }
}

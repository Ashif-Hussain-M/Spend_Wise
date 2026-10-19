package com.spendwise.service.fakes;

import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.repository.CategoryRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeCategoryRepository implements CategoryRepository {

    private final Map<Long, Category> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);
    private final Map<Long, Integer> txCounts = new HashMap<>();

    @Override
    public Optional<Category> findById(Long categoryId) {
        return Optional.ofNullable(storage.get(categoryId));
    }

    @Override
    public Optional<Category> findByNameAndType(Long userId, String name, CategoryType type) {
        return storage.values().stream()
                .filter(c -> c.getUserId().equals(userId) &&
                        c.getCategoryName().equalsIgnoreCase(name.trim()) &&
                        c.getCategoryType() == type)
                .findFirst();
    }

    @Override
    public List<Category> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .toList();
    }

    @Override
    public List<Category> findByUserIdAndType(Long userId, CategoryType type) {
        return storage.values().stream()
                .filter(c -> c.getUserId().equals(userId) && c.getCategoryType() == type)
                .toList();
    }

    @Override
    public Category save(Category category) {
        if (category.getCategoryId() == null) {
            category.setCategoryId(idGen.getAndIncrement());
        }
        storage.put(category.getCategoryId(), category);
        return category;
    }

    @Override
    public boolean update(Category category) {
        if (storage.containsKey(category.getCategoryId())) {
            storage.put(category.getCategoryId(), category);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteById(Long categoryId) {
        return storage.remove(categoryId) != null;
    }

    @Override
    public int countTransactions(Long categoryId) {
        return txCounts.getOrDefault(categoryId, 0);
    }

    public void setTransactionCount(Long categoryId, int count) {
        txCounts.put(categoryId, count);
    }
}

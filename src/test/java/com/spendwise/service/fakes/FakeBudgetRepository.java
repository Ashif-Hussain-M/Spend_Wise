package com.spendwise.service.fakes;

import com.spendwise.model.Budget;
import com.spendwise.repository.BudgetRepository;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeBudgetRepository implements BudgetRepository {

    private final Map<Long, Budget> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<Budget> findById(Long budgetId) {
        return Optional.ofNullable(storage.get(budgetId));
    }

    @Override
    public Optional<Budget> findOverallBudget(Long userId, int year, int month) {
        return storage.values().stream()
                .filter(b -> b.getUserId().equals(userId) && b.getBudgetYear() == year &&
                        b.getBudgetMonth() == month && b.getCategoryId() == null)
                .findFirst();
    }

    @Override
    public Optional<Budget> findCategoryBudget(Long userId, Long categoryId, int year, int month) {
        return storage.values().stream()
                .filter(b -> b.getUserId().equals(userId) && b.getBudgetYear() == year &&
                        b.getBudgetMonth() == month && Objects.equals(b.getCategoryId(), categoryId))
                .findFirst();
    }

    @Override
    public List<Budget> findByMonth(Long userId, int year, int month) {
        return storage.values().stream()
                .filter(b -> b.getUserId().equals(userId) && b.getBudgetYear() == year && b.getBudgetMonth() == month)
                .toList();
    }

    @Override
    public List<Budget> findAllByUserId(Long userId) {
        return storage.values().stream()
                .filter(b -> b.getUserId().equals(userId))
                .toList();
    }

    @Override
    public Budget save(Budget budget) {
        if (budget.getBudgetId() == null) {
            budget.setBudgetId(idGen.getAndIncrement());
        }
        storage.put(budget.getBudgetId(), budget);
        return budget;
    }

    @Override
    public boolean update(Budget budget) {
        if (storage.containsKey(budget.getBudgetId())) {
            storage.put(budget.getBudgetId(), budget);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteById(Long budgetId) {
        return storage.remove(budgetId) != null;
    }
}

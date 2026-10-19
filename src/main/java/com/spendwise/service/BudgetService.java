package com.spendwise.service;

import com.spendwise.model.Budget;
import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryRepository categoryRepository;

    public BudgetService(BudgetRepository budgetRepository, CategoryRepository categoryRepository) {
        this.budgetRepository = budgetRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Budget> getBudgetsForMonth(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            return budgetRepository.findByMonth(userId, year, month);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load budgets for month: " + e.getMessage(), e);
        }
    }

    public List<Budget> getAllBudgets(Long userId) {
        try {
            return budgetRepository.findAllByUserId(userId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load all budgets: " + e.getMessage(), e);
        }
    }

    public Optional<Budget> getOverallBudget(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            return budgetRepository.findOverallBudget(userId, year, month);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load overall budget: " + e.getMessage(), e);
        }
    }

    public Optional<Budget> getCategoryBudget(Long userId, Long categoryId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            return budgetRepository.findCategoryBudget(userId, categoryId, year, month);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load category budget: " + e.getMessage(), e);
        }
    }

    public Budget setOverallBudget(Long userId, int year, int month, BigDecimal amount) {
        validateBudgetInput(userId, year, month, amount);
        try {
            Optional<Budget> existing = budgetRepository.findOverallBudget(userId, year, month);
            if (existing.isPresent()) {
                Budget b = existing.get();
                b.setBudgetAmount(MoneyUtil.of(amount));
                budgetRepository.update(b);
                return b;
            } else {
                Budget b = new Budget();
                b.setUserId(userId);
                b.setCategoryId(null);
                b.setBudgetMonth(month);
                b.setBudgetYear(year);
                b.setBudgetAmount(MoneyUtil.of(amount));
                return budgetRepository.save(b);
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to set overall budget: " + e.getMessage(), e);
        }
    }

    public Budget setCategoryBudget(Long userId, Long categoryId, int year, int month, BigDecimal amount) {
        validateBudgetInput(userId, year, month, amount);
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Please select a valid expense category.");
        }
        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Selected category does not exist."));
        if (cat.getCategoryType() != CategoryType.EXPENSE) {
            throw new ValidationException("Budgets can only be set for expense categories.");
        }

        try {
            Optional<Budget> existing = budgetRepository.findCategoryBudget(userId, categoryId, year, month);
            if (existing.isPresent()) {
                Budget b = existing.get();
                b.setBudgetAmount(MoneyUtil.of(amount));
                budgetRepository.update(b);
                return b;
            } else {
                Budget b = new Budget();
                b.setUserId(userId);
                b.setCategoryId(categoryId);
                b.setBudgetMonth(month);
                b.setBudgetYear(year);
                b.setBudgetAmount(MoneyUtil.of(amount));
                return budgetRepository.save(b);
            }
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to set category budget: " + e.getMessage(), e);
        }
    }

    public boolean deleteBudget(Long budgetId) {
        if (budgetId == null || budgetId <= 0) {
            throw new ValidationException("Invalid budget identifier.");
        }
        try {
            return budgetRepository.deleteById(budgetId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to delete budget: " + e.getMessage(), e);
        }
    }

    private void validateBudgetInput(Long userId, int year, int month, BigDecimal amount) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Valid user identifier is required.");
        }
        validateMonthAndYear(month, year);
        if (amount == null) {
            throw new ValidationException("Budget amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Budget amount must be greater than zero.");
        }
    }

    private void validateMonthAndYear(int month, int year) {
        if (month < 1 || month > 12) {
            throw new ValidationException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Year must be between 2000 and 2100.");
        }
    }
}

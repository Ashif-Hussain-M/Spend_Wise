package com.spendwise.repository;

import com.spendwise.model.Budget;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository {

    Optional<Budget> findById(Long budgetId);

    Optional<Budget> findOverallBudget(Long userId, int year, int month);

    Optional<Budget> findCategoryBudget(Long userId, Long categoryId, int year, int month);

    List<Budget> findByMonth(Long userId, int year, int month);

    List<Budget> findAllByUserId(Long userId);

    Budget save(Budget budget);

    boolean update(Budget budget);

    boolean deleteById(Long budgetId);
}

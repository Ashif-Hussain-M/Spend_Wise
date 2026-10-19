package com.spendwise.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Budget {
    private Long budgetId;
    private Long userId;
    private Long categoryId;
    private String categoryName;
    private int budgetMonth;
    private int budgetYear;
    private BigDecimal budgetAmount;
    private Long categoryKey;
    private LocalDateTime createdAt;

    public Budget() {
    }

    public Budget(Long budgetId, Long userId, Long categoryId, String categoryName,
                  int budgetMonth, int budgetYear, BigDecimal budgetAmount,
                  Long categoryKey, LocalDateTime createdAt) {
        this.budgetId = budgetId;
        this.userId = userId;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.budgetMonth = budgetMonth;
        this.budgetYear = budgetYear;
        this.budgetAmount = budgetAmount;
        this.categoryKey = categoryKey;
        this.createdAt = createdAt;
    }

    public Long getBudgetId() {
        return budgetId;
    }

    public void setBudgetId(Long budgetId) {
        this.budgetId = budgetId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public int getBudgetMonth() {
        return budgetMonth;
    }

    public void setBudgetMonth(int budgetMonth) {
        this.budgetMonth = budgetMonth;
    }

    public int getBudgetYear() {
        return budgetYear;
    }

    public void setBudgetYear(int budgetYear) {
        this.budgetYear = budgetYear;
    }

    public BigDecimal getBudgetAmount() {
        return budgetAmount;
    }

    public void setBudgetAmount(BigDecimal budgetAmount) {
        this.budgetAmount = budgetAmount;
    }

    public Long getCategoryKey() {
        return categoryKey;
    }

    public void setCategoryKey(Long categoryKey) {
        this.categoryKey = categoryKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isOverallBudget() {
        return categoryId == null;
    }
}

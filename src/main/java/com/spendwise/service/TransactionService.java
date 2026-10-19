package com.spendwise.service;

import com.spendwise.model.Category;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.TransactionRepository;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository transactionRepository, CategoryRepository categoryRepository) {
        this.transactionRepository = transactionRepository;
        this.categoryRepository = categoryRepository;
    }

    public List<Transaction> getTransactions(Long userId) {
        try {
            return transactionRepository.findByUserId(userId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load transactions: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getRecentTransactions(Long userId, int limit) {
        if (limit <= 0) {
            limit = 10;
        }
        try {
            return transactionRepository.findRecent(userId, limit);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load recent transactions: " + e.getMessage(), e);
        }
    }

    public List<Transaction> getFilteredTransactions(Long userId,
                                                    LocalDate startDate,
                                                    LocalDate endDate,
                                                    Long categoryId,
                                                    TransactionType type,
                                                    BigDecimal minAmount,
                                                    BigDecimal maxAmount,
                                                    String searchDescription) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ValidationException("Start date cannot be after end date.");
        }
        if (minAmount != null && maxAmount != null && minAmount.compareTo(maxAmount) > 0) {
            throw new ValidationException("Minimum amount cannot be greater than maximum amount.");
        }
        try {
            return transactionRepository.findFiltered(userId, startDate, endDate, categoryId, type, minAmount, maxAmount, searchDescription);
        } catch (Exception e) {
            throw new DataAccessException("Unable to filter transactions: " + e.getMessage(), e);
        }
    }

    public Optional<Transaction> getTransactionById(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new ValidationException("Invalid transaction identifier.");
        }
        try {
            return transactionRepository.findById(transactionId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to find transaction: " + e.getMessage(), e);
        }
    }

    public Transaction addTransaction(Long userId, Long categoryId, TransactionType type,
                                      BigDecimal amount, LocalDate date, String description) {
        validateTransactionData(userId, categoryId, type, amount, date, description);
        try {
            Transaction tx = new Transaction();
            tx.setUserId(userId);
            tx.setCategoryId(categoryId);
            tx.setTransactionType(type);
            tx.setAmount(MoneyUtil.of(amount));
            tx.setTransactionDate(date);
            tx.setDescription(description.trim());
            return transactionRepository.save(tx);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to save transaction: " + e.getMessage(), e);
        }
    }

    public boolean updateTransaction(Long userId, Long transactionId, Long categoryId, TransactionType type,
                                     BigDecimal amount, LocalDate date, String description) {
        if (transactionId == null || transactionId <= 0) {
            throw new ValidationException("Invalid transaction identifier.");
        }
        validateTransactionData(userId, categoryId, type, amount, date, description);
        try {
            Transaction existing = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new ValidationException("Transaction not found."));

            existing.setCategoryId(categoryId);
            existing.setTransactionType(type);
            existing.setAmount(MoneyUtil.of(amount));
            existing.setTransactionDate(date);
            existing.setDescription(description.trim());
            return transactionRepository.update(existing);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to update transaction: " + e.getMessage(), e);
        }
    }

    public boolean deleteTransaction(Long transactionId) {
        if (transactionId == null || transactionId <= 0) {
            throw new ValidationException("Invalid transaction identifier.");
        }
        try {
            return transactionRepository.deleteById(transactionId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to delete transaction: " + e.getMessage(), e);
        }
    }

    private void validateTransactionData(Long userId, Long categoryId, TransactionType type,
                                         BigDecimal amount, LocalDate date, String description) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Valid user identifier is required.");
        }
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Please select a category.");
        }
        if (type == null) {
            throw new ValidationException("Transaction type is required.");
        }
        if (amount == null) {
            throw new ValidationException("Amount is required.");
        }
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("Amount must be greater than zero.");
        }
        if (date == null) {
            throw new ValidationException("Transaction date is required.");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new ValidationException("Description cannot be empty.");
        }
        if (description.trim().length() > 255) {
            throw new ValidationException("Description cannot exceed 255 characters.");
        }

        Category cat = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ValidationException("Selected category does not exist."));

        if (!cat.getCategoryType().name().equals(type.name())) {
            throw new ValidationException("Category '" + cat.getCategoryName() + "' is of type " +
                    cat.getCategoryType().name().toLowerCase() + " and cannot be used for " +
                    type.name().toLowerCase() + " transactions.");
        }
    }
}

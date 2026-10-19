package com.spendwise.repository;

import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository {

    Optional<Transaction> findById(Long transactionId);

    List<Transaction> findByUserId(Long userId);

    List<Transaction> findFiltered(Long userId,
                                  LocalDate startDate,
                                  LocalDate endDate,
                                  Long categoryId,
                                  TransactionType type,
                                  BigDecimal minAmount,
                                  BigDecimal maxAmount,
                                  String searchDescription);

    List<Transaction> findRecent(Long userId, int limit);

    Transaction save(Transaction transaction);

    boolean update(Transaction transaction);

    boolean deleteById(Long transactionId);
}

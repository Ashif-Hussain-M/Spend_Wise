package com.spendwise.service.fakes;

import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.repository.TransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

public class FakeTransactionRepository implements TransactionRepository {

    private final Map<Long, Transaction> storage = new HashMap<>();
    private final AtomicLong idGen = new AtomicLong(1);

    @Override
    public Optional<Transaction> findById(Long transactionId) {
        return Optional.ofNullable(storage.get(transactionId));
    }

    @Override
    public List<Transaction> findByUserId(Long userId) {
        return storage.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .toList();
    }

    @Override
    public List<Transaction> findFiltered(Long userId, LocalDate startDate, LocalDate endDate,
                                          Long categoryId, TransactionType type,
                                          BigDecimal minAmount, BigDecimal maxAmount,
                                          String searchDescription) {
        return storage.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .filter(t -> startDate == null || !t.getTransactionDate().isBefore(startDate))
                .filter(t -> endDate == null || !t.getTransactionDate().isAfter(endDate))
                .filter(t -> categoryId == null || t.getCategoryId().equals(categoryId))
                .filter(t -> type == null || t.getTransactionType() == type)
                .filter(t -> minAmount == null || t.getAmount().compareTo(minAmount) >= 0)
                .filter(t -> maxAmount == null || t.getAmount().compareTo(maxAmount) <= 0)
                .filter(t -> searchDescription == null || t.getDescription().toLowerCase().contains(searchDescription.toLowerCase()))
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .toList();
    }

    @Override
    public List<Transaction> findRecent(Long userId, int limit) {
        return storage.values().stream()
                .filter(t -> t.getUserId().equals(userId))
                .sorted((a, b) -> b.getTransactionDate().compareTo(a.getTransactionDate()))
                .limit(limit)
                .toList();
    }

    @Override
    public Transaction save(Transaction transaction) {
        if (transaction.getTransactionId() == null) {
            transaction.setTransactionId(idGen.getAndIncrement());
        }
        storage.put(transaction.getTransactionId(), transaction);
        return transaction;
    }

    @Override
    public boolean update(Transaction transaction) {
        if (storage.containsKey(transaction.getTransactionId())) {
            storage.put(transaction.getTransactionId(), transaction);
            return true;
        }
        return false;
    }

    @Override
    public boolean deleteById(Long transactionId) {
        return storage.remove(transactionId) != null;
    }
}

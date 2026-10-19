package com.spendwise.repository;

import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository {

    Optional<Category> findById(Long categoryId);

    Optional<Category> findByNameAndType(Long userId, String name, CategoryType type);

    List<Category> findByUserId(Long userId);

    List<Category> findByUserIdAndType(Long userId, CategoryType type);

    Category save(Category category);

    boolean update(Category category);

    boolean deleteById(Long categoryId);

    int countTransactions(Long categoryId);
}

package com.spendwise.service;

import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.repository.CategoryRepository;

import java.util.List;
import java.util.Optional;

public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getCategories(Long userId) {
        try {
            return categoryRepository.findByUserId(userId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load categories: " + e.getMessage(), e);
        }
    }

    public List<Category> getCategoriesByType(Long userId, CategoryType type) {
        if (type == null) {
            throw new ValidationException("Category type is required.");
        }
        try {
            return categoryRepository.findByUserIdAndType(userId, type);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load categories by type: " + e.getMessage(), e);
        }
    }

    public Optional<Category> getCategoryById(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Invalid category identifier.");
        }
        try {
            return categoryRepository.findById(categoryId);
        } catch (Exception e) {
            throw new DataAccessException("Unable to find category: " + e.getMessage(), e);
        }
    }

    public Category createCategory(Long userId, String name, CategoryType type) {
        validateCategoryInput(userId, name, type);
        try {
            Optional<Category> existing = categoryRepository.findByNameAndType(userId, name.trim(), type);
            if (existing.isPresent()) {
                throw new ValidationException("A category named '" + name.trim() + "' already exists under " + type.name().toLowerCase() + ".");
            }
            Category cat = new Category();
            cat.setUserId(userId);
            cat.setCategoryName(name.trim());
            cat.setCategoryType(type);
            return categoryRepository.save(cat);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to create category: " + e.getMessage(), e);
        }
    }

    public boolean updateCategory(Long userId, Long categoryId, String newName, CategoryType newType) {
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Invalid category identifier.");
        }
        validateCategoryInput(userId, newName, newType);
        try {
            Category existing = categoryRepository.findById(categoryId)
                    .orElseThrow(() -> new ValidationException("Category not found."));

            if (existing.getCategoryType() != newType && categoryRepository.countTransactions(categoryId) > 0) {
                throw new ValidationException("Cannot change category type because transactions are already recorded under it.");
            }

            Optional<Category> duplicate = categoryRepository.findByNameAndType(userId, newName.trim(), newType);
            if (duplicate.isPresent() && !duplicate.get().getCategoryId().equals(categoryId)) {
                throw new ValidationException("A category named '" + newName.trim() + "' already exists under " + newType.name().toLowerCase() + ".");
            }

            existing.setCategoryName(newName.trim());
            existing.setCategoryType(newType);
            return categoryRepository.update(existing);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to update category: " + e.getMessage(), e);
        }
    }

    public boolean deleteCategory(Long categoryId) {
        if (categoryId == null || categoryId <= 0) {
            throw new ValidationException("Invalid category identifier.");
        }
        try {
            int txCount = categoryRepository.countTransactions(categoryId);
            if (txCount > 0) {
                throw new ValidationException("Category is in use by " + txCount + " transaction(s) and cannot be deleted.");
            }
            return categoryRepository.deleteById(categoryId);
        } catch (ValidationException ve) {
            throw ve;
        } catch (Exception e) {
            throw new DataAccessException("Unable to delete category: " + e.getMessage(), e);
        }
    }

    private void validateCategoryInput(Long userId, String name, CategoryType type) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Valid user identifier is required.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new ValidationException("Category name cannot be empty.");
        }
        if (name.trim().length() > 100) {
            throw new ValidationException("Category name cannot exceed 100 characters.");
        }
        if (type == null) {
            throw new ValidationException("Category type is required.");
        }
    }
}

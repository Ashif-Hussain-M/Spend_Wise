package com.spendwise.service;

import com.spendwise.model.BudgetStatus;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.repository.ReportRepository;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public class ReportService {

    private final ReportRepository reportRepository;
    private double warningThresholdPercent = 80.0;

    public ReportService(ReportRepository reportRepository) {
        this.reportRepository = reportRepository;
    }

    public double getWarningThresholdPercent() {
        return warningThresholdPercent;
    }

    public void setWarningThresholdPercent(double warningThresholdPercent) {
        if (warningThresholdPercent <= 0.0 || warningThresholdPercent > 100.0) {
            throw new ValidationException("Warning threshold must be between 1% and 100%.");
        }
        this.warningThresholdPercent = warningThresholdPercent;
    }

    public MonthlySummary getMonthlySummary(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            return reportRepository.getMonthlySummary(userId, year, month);
        } catch (Exception e) {
            throw new DataAccessException("Unable to calculate monthly summary: " + e.getMessage(), e);
        }
    }

    public List<CategorySpending> getCategorySpending(Long userId, int year, int month, CategoryType type) {
        validateMonthAndYear(month, year);
        try {
            return reportRepository.getCategorySpending(userId, year, month, type);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load category spending: " + e.getMessage(), e);
        }
    }

    public List<MonthComparison> getMonthlyHistory(Long userId, int numberOfMonths, LocalDate referenceDate) {
        if (numberOfMonths <= 0) {
            numberOfMonths = 6;
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }
        try {
            return reportRepository.getMonthlyHistory(userId, numberOfMonths, referenceDate);
        } catch (Exception e) {
            throw new DataAccessException("Unable to load monthly history: " + e.getMessage(), e);
        }
    }

    public List<BudgetStatus> getCategoryBudgetStatuses(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            List<BudgetStatus> rawList = reportRepository.getBudgetStatuses(userId, year, month);
            return rawList.stream()
                    .map(b -> adjustStatusWithConfiguredThreshold(b, warningThresholdPercent))
                    .toList();
        } catch (Exception e) {
            throw new DataAccessException("Unable to load category budget statuses: " + e.getMessage(), e);
        }
    }

    public Optional<BudgetStatus> getOverallBudgetStatus(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            Optional<BudgetStatus> raw = reportRepository.getOverallBudgetStatus(userId, year, month);
            return raw.map(b -> adjustStatusWithConfiguredThreshold(b, warningThresholdPercent));
        } catch (Exception e) {
            throw new DataAccessException("Unable to load overall budget status: " + e.getMessage(), e);
        }
    }

    public boolean hasDataInMonth(Long userId, int year, int month) {
        validateMonthAndYear(month, year);
        try {
            return reportRepository.hasDataInMonth(userId, year, month);
        } catch (Exception e) {
            throw new DataAccessException("Unable to verify monthly data: " + e.getMessage(), e);
        }
    }

    private BudgetStatus adjustStatusWithConfiguredThreshold(BudgetStatus original, double threshold) {
        double pct = original.utilizationPercentage();
        String label;
        if (pct > 100.0) {
            label = "Over budget";
        } else if (pct >= threshold) {
            label = "Near limit";
        } else {
            label = "On track";
        }
        return new BudgetStatus(
                original.budgetId(),
                original.categoryId(),
                original.categoryName(),
                original.month(),
                original.year(),
                original.budgetAmount(),
                original.actualSpent(),
                original.remainingAmount(),
                original.utilizationPercentage(),
                label
        );
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

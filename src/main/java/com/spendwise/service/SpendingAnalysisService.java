package com.spendwise.service;

import com.spendwise.model.BudgetStatus;
import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.SimulatorResult;
import com.spendwise.model.SpendingForecast;
import com.spendwise.model.UnusualSpendingInsight;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.ReportRepository;
import com.spendwise.util.MoneyUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SpendingAnalysisService {

    private final ReportRepository reportRepository;
    private final CategoryRepository categoryRepository;

    private double unusualSpendingThresholdPercent = 30.0;
    private BigDecimal minAbsoluteIncrease = new BigDecimal("500.00");

    public SpendingAnalysisService(ReportRepository reportRepository, CategoryRepository categoryRepository) {
        this.reportRepository = reportRepository;
        this.categoryRepository = categoryRepository;
    }

    public double getUnusualSpendingThresholdPercent() {
        return unusualSpendingThresholdPercent;
    }

    public void setUnusualSpendingThresholdPercent(double thresholdPercent) {
        if (thresholdPercent <= 0.0) {
            throw new ValidationException("Unusual spending threshold must be greater than zero.");
        }
        this.unusualSpendingThresholdPercent = thresholdPercent;
    }

    public BigDecimal getMinAbsoluteIncrease() {
        return minAbsoluteIncrease;
    }

    public void setMinAbsoluteIncrease(BigDecimal minAbsoluteIncrease) {
        if (minAbsoluteIncrease == null || minAbsoluteIncrease.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("Minimum absolute increase cannot be negative.");
        }
        this.minAbsoluteIncrease = minAbsoluteIncrease;
    }

    public List<UnusualSpendingInsight> detectUnusualSpending(Long userId, LocalDate today) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Valid user identifier is required.");
        }
        if (today == null) {
            today = LocalDate.now();
        }

        List<UnusualSpendingInsight> insights = new ArrayList<>();
        List<Category> expenseCategories = categoryRepository.findByUserIdAndType(userId, CategoryType.EXPENSE);

        int currentElapsedDay = today.getDayOfMonth();
        YearMonth currentYm = YearMonth.from(today);

        List<YearMonth> previousMonths = List.of(
                currentYm.minusMonths(1),
                currentYm.minusMonths(2),
                currentYm.minusMonths(3)
        );

        int monthsWithHistory = 0;
        for (YearMonth ym : previousMonths) {
            if (reportRepository.hasDataInMonth(userId, ym.getYear(), ym.getMonthValue())) {
                monthsWithHistory++;
            }
        }

        if (monthsWithHistory < 2) {
            return insights;
        }

        for (Category cat : expenseCategories) {
            BigDecimal currentSpending = reportRepository.getCategorySpendingInElapsedDayWindow(
                    userId, cat.getCategoryId(), today.getYear(), today.getMonthValue(), currentElapsedDay
            );

            if (currentSpending.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal baselineSum = BigDecimal.ZERO;
            int validBaselineMonths = 0;

            for (YearMonth prevYm : previousMonths) {
                if (reportRepository.hasDataInMonth(userId, prevYm.getYear(), prevYm.getMonthValue())) {
                    int prevWindowDay = Math.min(currentElapsedDay, prevYm.lengthOfMonth());
                    BigDecimal prevSpending = reportRepository.getCategorySpendingInElapsedDayWindow(
                            userId, cat.getCategoryId(), prevYm.getYear(), prevYm.getMonthValue(), prevWindowDay
                    );
                    baselineSum = baselineSum.add(prevSpending);
                    validBaselineMonths++;
                }
            }

            if (validBaselineMonths < 2) {
                continue;
            }

            BigDecimal baselineAvg = baselineSum.divide(BigDecimal.valueOf(validBaselineMonths), 2, RoundingMode.HALF_UP);
            BigDecimal increase = currentSpending.subtract(baselineAvg);

            if (increase.compareTo(minAbsoluteIncrease) > 0) {
                double pctDiff;
                if (baselineAvg.compareTo(BigDecimal.ZERO) > 0) {
                    pctDiff = increase.divide(baselineAvg, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100)).doubleValue();
                } else {
                    pctDiff = 100.0;
                }

                if (pctDiff >= unusualSpendingThresholdPercent) {
                    String reason = "Potential unusual spending pattern: Current " + cat.getCategoryName() +
                            " spending of " + MoneyUtil.formatRupees(currentSpending) +
                            " exceeds the historical baseline of " + MoneyUtil.formatRupees(baselineAvg) +
                            " by " + String.format("%.1f", pctDiff) + "% (+" + MoneyUtil.formatRupees(increase) +
                            ") across days 1 to " + currentElapsedDay + ".";

                    insights.add(new UnusualSpendingInsight(
                            cat.getCategoryId(),
                            cat.getCategoryName(),
                            MoneyUtil.of(currentSpending),
                            MoneyUtil.of(baselineAvg),
                            pctDiff,
                            MoneyUtil.of(increase),
                            reason
                    ));
                }
            }
        }

        return insights;
    }

    public SpendingForecast forecastMonthlySpending(Long userId, int year, int month, LocalDate referenceDate) {
        if (userId == null || userId <= 0) {
            throw new ValidationException("Valid user identifier is required.");
        }
        if (month < 1 || month > 12) {
            throw new ValidationException("Month must be between 1 and 12.");
        }
        if (year < 2000 || year > 2100) {
            throw new ValidationException("Year must be between 2000 and 2100.");
        }
        if (referenceDate == null) {
            referenceDate = LocalDate.now();
        }

        YearMonth targetYm = YearMonth.of(year, month);
        YearMonth currentYm = YearMonth.from(referenceDate);
        int totalDaysInMonth = targetYm.lengthOfMonth();

        Optional<BudgetStatus> overallBudgetOpt = reportRepository.getOverallBudgetStatus(userId, year, month);
        BigDecimal budgetAmount = overallBudgetOpt.map(BudgetStatus::budgetAmount).orElse(null);

        if (targetYm.isBefore(currentYm)) {
            MonthlySummary summary = reportRepository.getMonthlySummary(userId, year, month);
            BigDecimal actual = summary.totalExpense();
            BigDecimal variance = budgetAmount != null ? budgetAmount.subtract(actual) : null;
            return new SpendingForecast(
                    year, month, totalDaysInMonth, totalDaysInMonth,
                    actual, actual, budgetAmount, variance, true,
                    "Actual spending (past month concluded)."
            );
        }

        if (targetYm.isAfter(currentYm)) {
            return new SpendingForecast(
                    year, month, 0, totalDaysInMonth,
                    MoneyUtil.zero(), MoneyUtil.zero(), budgetAmount, budgetAmount, false,
                    "Estimate: Future month with no spending recorded yet."
            );
        }

        int elapsedDays = referenceDate.getDayOfMonth();
        LocalDate startDate = targetYm.atDay(1);
        BigDecimal mtdSpending = reportRepository.getCategorySpendingInDateRange(userId, null, startDate, referenceDate);

        if (mtdSpending == null || mtdSpending.compareTo(BigDecimal.ZERO) == 0) {
            MonthlySummary summary = reportRepository.getMonthlySummary(userId, year, month);
            mtdSpending = summary.totalExpense();
        }

        BigDecimal projected;
        String explanation;

        if (mtdSpending.compareTo(BigDecimal.ZERO) == 0) {
            projected = MoneyUtil.zero();
            explanation = "Estimate: No spending recorded through day " + elapsedDays + " of " + totalDaysInMonth + ".";
        } else {
            BigDecimal dailyAvg = mtdSpending.divide(BigDecimal.valueOf(elapsedDays), 4, RoundingMode.HALF_UP);
            projected = dailyAvg.multiply(BigDecimal.valueOf(totalDaysInMonth)).setScale(2, RoundingMode.HALF_UP);
            explanation = "Estimate: Projected total of " + MoneyUtil.formatRupees(projected) +
                    " based on " + MoneyUtil.formatRupees(mtdSpending) +
                    " spent through day " + elapsedDays + " of " + totalDaysInMonth + " days.";
        }

        BigDecimal variance = budgetAmount != null ? budgetAmount.subtract(projected) : null;

        return new SpendingForecast(
                year, month, elapsedDays, totalDaysInMonth,
                MoneyUtil.of(mtdSpending), projected, budgetAmount, variance, false, explanation
        );
    }

    public SimulatorResult simulateSavings(BigDecimal currentTotalExpense, Map<Long, BigDecimal> categorySpendings, Map<Long, Double> percentageReductions) {
        if (currentTotalExpense == null) {
            currentTotalExpense = MoneyUtil.zero();
        }
        BigDecimal totalSavings = MoneyUtil.zero();
        Map<Long, BigDecimal> savingsPerCategory = new HashMap<>();

        if (categorySpendings != null && percentageReductions != null) {
            for (Map.Entry<Long, Double> entry : percentageReductions.entrySet()) {
                Long categoryId = entry.getKey();
                Double reductionPercent = entry.getValue();
                if (reductionPercent != null && reductionPercent > 0.0) {
                    if (reductionPercent > 100.0) {
                        reductionPercent = 100.0;
                    }
                    BigDecimal spending = categorySpendings.getOrDefault(categoryId, BigDecimal.ZERO);
                    if (spending.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal savings = spending.multiply(BigDecimal.valueOf(reductionPercent / 100.0))
                                .setScale(2, RoundingMode.HALF_UP);
                        savingsPerCategory.put(categoryId, savings);
                        totalSavings = totalSavings.add(savings);
                    }
                }
            }
        }

        BigDecimal projectedExpense = currentTotalExpense.subtract(totalSavings);
        if (projectedExpense.compareTo(BigDecimal.ZERO) < 0) {
            projectedExpense = MoneyUtil.zero();
        }

        return new SimulatorResult(
                MoneyUtil.of(currentTotalExpense),
                MoneyUtil.of(totalSavings),
                MoneyUtil.of(projectedExpense),
                savingsPerCategory
        );
    }
}

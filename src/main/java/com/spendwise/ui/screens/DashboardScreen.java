package com.spendwise.ui.screens;

import com.spendwise.model.BudgetStatus;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.SpendingForecast;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.model.UnusualSpendingInsight;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.service.TransactionService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.ChartPanel;
import com.spendwise.ui.components.EmptyStatePanel;
import com.spendwise.ui.components.ProgressIndicator;
import com.spendwise.ui.components.SectionHeader;
import com.spendwise.ui.components.StyledTable;
import com.spendwise.ui.components.SummaryCard;
import com.spendwise.ui.theme.AppIcons;
import com.spendwise.ui.theme.AppTheme;
import com.spendwise.util.MoneyUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class DashboardScreen extends JPanel implements BaseScreen {

    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;
    private final TransactionService transactionService;

    private int currentYear;
    private int currentMonth;

    private final SummaryCard incomeCard;
    private final SummaryCard expenseCard;
    private final SummaryCard savingsCard;
    private final SummaryCard budgetCard;

    private final ChartPanel categoryChart;
    private final ChartPanel historyChart;

    private final ProgressIndicator budgetProgress;
    private final JLabel budgetProgressLabel;
    private final JPanel insightsContainer;

    private final DefaultTableModel recentTxModel;
    private final StyledTable recentTxTable;

    public DashboardScreen(ReportService reportService,
                           SpendingAnalysisService analysisService,
                           TransactionService transactionService) {
        this.reportService = reportService;
        this.analysisService = analysisService;
        this.transactionService = transactionService;

        this.currentYear = LocalDate.now().getYear();
        this.currentMonth = LocalDate.now().getMonthValue();

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 24, 0));

        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        incomeCard = new SummaryCard("TOTAL INCOME", "Rs 0.00", "Loading...", AppIcons.add(18, AppTheme.SUCCESS), AppTheme.SUCCESS);
        expenseCard = new SummaryCard("TOTAL EXPENSES", "Rs 0.00", "Loading...", AppIcons.delete(18, AppTheme.DANGER), AppTheme.DANGER);
        savingsCard = new SummaryCard("NET SAVINGS", "Rs 0.00", "Savings Rate: 0%", AppIcons.dashboard(18, AppTheme.BLUE_ACCENT), AppTheme.BLUE_ACCENT);
        budgetCard = new SummaryCard("REMAINING BUDGET", "No budget", "Loading...", AppIcons.budgets(18, AppTheme.WARNING), AppTheme.WARNING);

        cardsPanel.add(incomeCard);
        cardsPanel.add(expenseCard);
        cardsPanel.add(savingsCard);
        cardsPanel.add(budgetCard);
        content.add(cardsPanel);

        content.add(Box.createVerticalStrut(18));

        JPanel budgetBanner = new JPanel(new BorderLayout(16, 0));
        budgetBanner.setBackground(AppTheme.CARD_BG);
        budgetBanner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));
        budgetBanner.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));

        JLabel bannerTitle = new JLabel("Monthly Budget Utilization:");
        bannerTitle.setFont(AppTheme.FONT_BODY_BOLD);
        bannerTitle.setForeground(AppTheme.TEXT_SECONDARY);

        budgetProgress = new ProgressIndicator(0.0);
        budgetProgress.setPreferredSize(new Dimension(280, 10));

        budgetProgressLabel = new JLabel("0% (On track)");
        budgetProgressLabel.setFont(AppTheme.FONT_BODY_BOLD);
        budgetProgressLabel.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel progressWrap = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        progressWrap.setOpaque(false);
        progressWrap.add(budgetProgress);
        progressWrap.add(budgetProgressLabel);

        budgetBanner.add(bannerTitle, BorderLayout.WEST);
        budgetBanner.add(progressWrap, BorderLayout.EAST);
        content.add(budgetBanner);

        content.add(Box.createVerticalStrut(20));

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        chartsPanel.setPreferredSize(new Dimension(Integer.MAX_VALUE, 280));
        chartsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 280));

        categoryChart = new ChartPanel();
        historyChart = new ChartPanel();

        chartsPanel.add(categoryChart);
        chartsPanel.add(historyChart);
        content.add(chartsPanel);

        content.add(Box.createVerticalStrut(20));

        SectionHeader insightsHeader = new SectionHeader("Spending Intelligence", "Rule-based forecasts and unusual spending detection");
        content.add(insightsHeader);

        insightsContainer = new JPanel(new GridLayout(1, 2, 16, 0));
        insightsContainer.setOpaque(false);
        insightsContainer.setMaximumSize(new Dimension(Integer.MAX_VALUE, 130));
        content.add(insightsContainer);

        content.add(Box.createVerticalStrut(20));

        SectionHeader recentHeader = new SectionHeader("Recent Transactions", "Latest activity recorded across your accounts");
        content.add(recentHeader);

        String[] cols = {"Date", "Category", "Description", "Type", "Amount"};
        recentTxModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        recentTxTable = new StyledTable(recentTxModel);
        JScrollPane tableScroll = new JScrollPane(recentTxTable);
        tableScroll.setBackground(AppTheme.CARD_BG);
        tableScroll.getViewport().setBackground(AppTheme.CARD_BG);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(Integer.MAX_VALUE, 200));
        tableScroll.setMaximumSize(new Dimension(Integer.MAX_VALUE, 200));
        content.add(tableScroll);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
        loadDashboardData();
    }

    @Override
    public void onMonthChanged(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        loadDashboardData();
    }

    public void loadDashboardData() {
        AsyncWorker.execute(
                () -> {
                    MonthlySummary summary = reportService.getMonthlySummary(1L, currentYear, currentMonth);

                    YearMonth prevYm = YearMonth.of(currentYear, currentMonth).minusMonths(1);
                    MonthlySummary prevSummary = null;
                    if (reportService.hasDataInMonth(1L, prevYm.getYear(), prevYm.getMonthValue())) {
                        prevSummary = reportService.getMonthlySummary(1L, prevYm.getYear(), prevYm.getMonthValue());
                    }

                    Optional<BudgetStatus> overallBudget = reportService.getOverallBudgetStatus(1L, currentYear, currentMonth);
                    List<CategorySpending> spending = reportService.getCategorySpending(1L, currentYear, currentMonth, CategoryType.EXPENSE);
                    List<MonthComparison> history = reportService.getMonthlyHistory(1L, 6, LocalDate.of(currentYear, currentMonth, 15));

                    LocalDate forecastDate = LocalDate.now();
                    if (forecastDate.getYear() != currentYear || forecastDate.getMonthValue() != currentMonth) {
                        forecastDate = LocalDate.of(currentYear, currentMonth, YearMonth.of(currentYear, currentMonth).lengthOfMonth());
                    }
                    SpendingForecast forecast = analysisService.forecastMonthlySpending(1L, currentYear, currentMonth, forecastDate);
                    List<UnusualSpendingInsight> unusual = analysisService.detectUnusualSpending(1L, forecastDate);
                    List<Transaction> recent = transactionService.getRecentTransactions(1L, 6);

                    return new DashboardPayload(summary, prevSummary, overallBudget, spending, history, forecast, unusual, recent);
                },
                this::applyDashboardData,
                err -> {
                    incomeCard.setValue("Error");
                    incomeCard.setSubtitle(err.getMessage());
                }
        );
    }

    private void applyDashboardData(DashboardPayload data) {
        incomeCard.setValue(MoneyUtil.formatRupees(data.summary.totalIncome()));
        expenseCard.setValue(MoneyUtil.formatRupees(data.summary.totalExpense()));
        savingsCard.setValue(MoneyUtil.formatRupees(data.summary.netSavings()));

        if (data.summary.netSavings().signum() >= 0) {
            savingsCard.setValueColor(AppTheme.SUCCESS);
        } else {
            savingsCard.setValueColor(AppTheme.DANGER);
        }
        savingsCard.setSubtitle("Savings Rate: " + data.summary.savingsRate() + "%");

        if (data.prevSummary != null) {
            BigDecimal incomeDiff = data.summary.totalIncome().subtract(data.prevSummary.totalIncome());
            incomeCard.setSubtitle((incomeDiff.signum() >= 0 ? "+" : "") + MoneyUtil.formatRupees(incomeDiff) + " vs last month");

            BigDecimal expenseDiff = data.summary.totalExpense().subtract(data.prevSummary.totalExpense());
            expenseCard.setSubtitle((expenseDiff.signum() >= 0 ? "+" : "") + MoneyUtil.formatRupees(expenseDiff) + " vs last month");
        } else {
            incomeCard.setSubtitle("No previous month data");
            expenseCard.setSubtitle("No previous month data");
        }

        if (data.overallBudget.isPresent()) {
            BudgetStatus bs = data.overallBudget.get();
            budgetCard.setValue(MoneyUtil.formatRupees(bs.remainingAmount()));
            budgetCard.setSubtitle(bs.statusLabel() + " (" + String.format("%.1f", bs.utilizationPercentage()) + "% used)");

            budgetProgress.setPercentage(bs.utilizationPercentage());
            budgetProgressLabel.setText(String.format("%.1f%% (%s)", bs.utilizationPercentage(), bs.statusLabel()));
            if (bs.utilizationPercentage() > 100.0) {
                budgetProgressLabel.setForeground(AppTheme.DANGER);
            } else if (bs.utilizationPercentage() >= 80.0) {
                budgetProgressLabel.setForeground(AppTheme.WARNING);
            } else {
                budgetProgressLabel.setForeground(AppTheme.SUCCESS);
            }
        } else {
            budgetCard.setValue("No budget");
            budgetCard.setSubtitle("Click Budgets to set one");
            budgetProgress.setPercentage(0.0);
            budgetProgressLabel.setText("No budget set for this month");
            budgetProgressLabel.setForeground(AppTheme.TEXT_MUTED);
        }

        String monthTitle = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYear;
        categoryChart.displayCategoryPieChart("Expenses by Category — " + monthTitle, data.spending);
        historyChart.displayIncomeVsExpenseBarChart("6-Month Financial Trend", data.history);

        updateInsightsPanel(data.forecast, data.unusual);

        recentTxModel.setRowCount(0);
        for (Transaction t : data.recent) {
            String typeIndicator = t.getTransactionType() == TransactionType.INCOME ? "[+] Income" : "[-] Expense";
            recentTxModel.addRow(new Object[]{
                    t.getTransactionDate().toString(),
                    t.getCategoryName(),
                    t.getDescription(),
                    typeIndicator,
                    MoneyUtil.formatRupees(t.getAmount())
            });
        }
    }

    private void updateInsightsPanel(SpendingForecast forecast, List<UnusualSpendingInsight> unusual) {
        insightsContainer.removeAll();

        JPanel forecastCard = createInsightCard(
                "Spending Forecast",
                forecast.explanation(),
                "Projected: " + MoneyUtil.formatRupees(forecast.projectedTotalSpending()),
                AppTheme.BLUE_ACCENT
        );
        insightsContainer.add(forecastCard);

        String unusualTitle = "Unusual Spending";
        String unusualDesc;
        Color accent = AppTheme.PRIMARY_ACCENT;

        if (unusual == null || unusual.isEmpty()) {
            unusualDesc = "Spending patterns across all categories are consistent with historical baselines.";
        } else {
            UnusualSpendingInsight top = unusual.get(0);
            unusualDesc = top.reason();
            accent = AppTheme.WARNING;
        }

        JPanel unusualCard = createInsightCard(unusualTitle, unusualDesc,
                unusual.isEmpty() ? "Status: Normal" : "Alert: " + unusual.size() + " pattern(s) flagged", accent);
        insightsContainer.add(unusualCard);

        insightsContainer.revalidate();
        insightsContainer.repaint();
    }

    private JPanel createInsightCard(String title, String desc, String badge, Color accent) {
        JPanel card = new JPanel(new BorderLayout(8, 8));
        card.setBackground(AppTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(12, 16, 12, 16)
        ));

        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_SUBHEADER);
        titleLbl.setForeground(accent);

        JLabel badgeLbl = new JLabel(badge);
        badgeLbl.setFont(AppTheme.FONT_SMALL);
        badgeLbl.setForeground(AppTheme.TEXT_SECONDARY);

        header.add(titleLbl, BorderLayout.WEST);
        header.add(badgeLbl, BorderLayout.EAST);
        card.add(header, BorderLayout.NORTH);

        JLabel descLbl = new JLabel("<html><body style='width: 320px;'>" + desc + "</body></html>");
        descLbl.setFont(AppTheme.FONT_BODY);
        descLbl.setForeground(AppTheme.TEXT_SECONDARY);
        card.add(descLbl, BorderLayout.CENTER);

        return card;
    }

    private record DashboardPayload(
            MonthlySummary summary,
            MonthlySummary prevSummary,
            Optional<BudgetStatus> overallBudget,
            List<CategorySpending> spending,
            List<MonthComparison> history,
            SpendingForecast forecast,
            List<UnusualSpendingInsight> unusual,
            List<Transaction> recent
    ) {
    }
}

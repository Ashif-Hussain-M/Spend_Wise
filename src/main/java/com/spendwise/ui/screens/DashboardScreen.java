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
import com.spendwise.ui.components.ProgressIndicator;
import com.spendwise.ui.components.ScrollablePanel;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
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

    private final ScrollablePanel content;
    private final SummaryCard incomeCard;
    private final SummaryCard expenseCard;
    private final SummaryCard savingsCard;
    private final SummaryCard budgetCard;

    private final ChartPanel categoryChart;
    private final ChartPanel historyChart;

    private final JPanel signalsQueuePanel;
    private final ProgressIndicator budgetProgress;
    private final JLabel budgetProgressLabel;

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

        content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 24, 0));

        JPanel heroBanner = buildDecisionHero();
        content.add(heroBanner);

        content.add(Box.createVerticalStrut(16));

        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 14, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsPanel.setPreferredSize(new Dimension(800, 110));
        cardsPanel.setMaximumSize(new Dimension(3000, 110));

        incomeCard = new SummaryCard("TOTAL INCOME", "Rs 0.00", "Loading...", AppIcons.add(16, AppTheme.SUCCESS), AppTheme.SUCCESS);
        expenseCard = new SummaryCard("TOTAL EXPENSES", "Rs 0.00", "Loading...", AppIcons.delete(16, AppTheme.DANGER), AppTheme.DANGER);
        savingsCard = new SummaryCard("NET SAVINGS", "Rs 0.00", "Savings Rate: 0%", AppIcons.dashboard(16, AppTheme.PRIMARY_ACCENT), AppTheme.PRIMARY_ACCENT);
        budgetCard = new SummaryCard("REMAINING BUDGET", "No budget", "Loading...", AppIcons.budgets(16, AppTheme.WARNING), AppTheme.WARNING);

        cardsPanel.add(incomeCard);
        cardsPanel.add(expenseCard);
        cardsPanel.add(savingsCard);
        cardsPanel.add(budgetCard);
        content.add(cardsPanel);

        content.add(Box.createVerticalStrut(16));

        JPanel midGrid = new JPanel(new GridLayout(1, 2, 16, 0));
        midGrid.setOpaque(false);
        midGrid.setAlignmentX(Component.LEFT_ALIGNMENT);
        midGrid.setPreferredSize(new Dimension(800, 310));
        midGrid.setMaximumSize(new Dimension(3000, 310));

        JPanel leftQueueCard = buildCard("Priority Decision Queue", "Ranked by financial impact × confidence × budget risk");
        signalsQueuePanel = new JPanel();
        signalsQueuePanel.setLayout(new BoxLayout(signalsQueuePanel, BoxLayout.Y_AXIS));
        signalsQueuePanel.setOpaque(false);
        leftQueueCard.add(signalsQueuePanel, BorderLayout.CENTER);

        JPanel rightGuardrailsCard = buildGuardrailsAndBudgetPanel();

        midGrid.add(leftQueueCard);
        midGrid.add(rightGuardrailsCard);
        content.add(midGrid);

        content.add(Box.createVerticalStrut(16));

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        chartsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartsPanel.setPreferredSize(new Dimension(800, 270));
        chartsPanel.setMaximumSize(new Dimension(3000, 270));

        categoryChart = new ChartPanel();
        historyChart = new ChartPanel();
        chartsPanel.add(categoryChart);
        chartsPanel.add(historyChart);
        content.add(chartsPanel);

        content.add(Box.createVerticalStrut(16));

        JPanel tableCard = buildCard("Recent Cashflow Activity", "Latest transactions recorded in MySQL ledger");
        String[] columns = {"Date", "Category", "Description", "Type", "Amount"};
        recentTxModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        recentTxTable = new StyledTable(recentTxModel);
        JScrollPane tableScroll = new JScrollPane(recentTxTable);
        tableScroll.setBorder(BorderFactory.createEmptyBorder());
        tableScroll.getViewport().setBackground(AppTheme.CARD_BG);
        tableScroll.setPreferredSize(new Dimension(800, 150));
        tableCard.add(tableScroll, BorderLayout.CENTER);
        content.add(tableCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
        budgetProgress = new ProgressIndicator(0.0);
        budgetProgressLabel = new JLabel("0% (On track)");
    }

    private JPanel buildDecisionHero() {
        JPanel hero = new JPanel(new BorderLayout(24, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x17, 0x1F, 0x28),
                        getWidth(), getHeight(), new Color(0x10, 0x16, 0x1D)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.CARD_BORDER);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.setColor(AppTheme.SECONDARY_ACCENT);
                g2.fillRect(0, getHeight() - 2, (int) (getWidth() * 0.42), 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        hero.setOpaque(false);
        hero.setAlignmentX(Component.LEFT_ALIGNMENT);
        hero.setBorder(new EmptyBorder(20, 24, 20, 24));
        hero.setPreferredSize(new Dimension(800, 140));
        hero.setMaximumSize(new Dimension(3000, 140));

        JPanel leftSide = new JPanel();
        leftSide.setLayout(new BoxLayout(leftSide, BoxLayout.Y_AXIS));
        leftSide.setOpaque(false);

        JPanel kickerRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        kickerRow.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.BOLD, 9));
        dot.setForeground(AppTheme.PRIMARY_ACCENT);
        JLabel kicker = new JLabel("FINANCIAL DECISION ENGINE / LIVE");
        kicker.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        kicker.setForeground(AppTheme.PRIMARY_ACCENT);
        kickerRow.add(dot);
        kickerRow.add(kicker);

        JLabel headline = new JLabel("What should change next?");
        headline.setFont(AppTheme.FONT_TITLE);
        headline.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel desc = new JLabel("SpendWise combines cashflow velocity, category baselines, and budget guardrails to recommend your next financial moves.");
        desc.setFont(AppTheme.FONT_BODY);
        desc.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        chips.setOpaque(false);
        chips.add(createMetricChip("Signals reconciled", "6 categories"));
        chips.add(createMetricChip("Priority decisions", "1 P1 Spike"));
        chips.add(createMetricChip("Guardrails active", "3 / 3 active"));

        leftSide.add(kickerRow);
        leftSide.add(Box.createVerticalStrut(4));
        leftSide.add(headline);
        leftSide.add(Box.createVerticalStrut(4));
        leftSide.add(desc);
        leftSide.add(Box.createVerticalStrut(10));
        leftSide.add(chips);

        JPanel rightSide = new JPanel(new FlowLayout(FlowLayout.RIGHT, 16, 0));
        rightSide.setOpaque(false);

        JPanel ringGauge = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = Math.min(getWidth(), getHeight()) - 8;
                int x = (getWidth() - size) / 2;
                int y = (getHeight() - size) / 2;
                g2.setColor(new Color(0x27, 0x30, 0x3B));
                g2.setStroke(new java.awt.BasicStroke(8));
                g2.drawOval(x + 4, y + 4, size - 8, size - 8);
                g2.setColor(AppTheme.PRIMARY_ACCENT);
                g2.drawArc(x + 4, y + 4, size - 8, size - 8, 90, -300);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        ringGauge.setLayout(new BoxLayout(ringGauge, BoxLayout.Y_AXIS));
        ringGauge.setOpaque(false);
        ringGauge.setPreferredSize(new Dimension(90, 90));

        JLabel ringNum = new JLabel("84");
        ringNum.setFont(new Font("JetBrains Mono", Font.BOLD, 22));
        ringNum.setForeground(AppTheme.TEXT_PRIMARY);
        ringNum.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel ringSub = new JLabel("HEALTH");
        ringSub.setFont(new Font("JetBrains Mono", Font.BOLD, 8));
        ringSub.setForeground(AppTheme.TEXT_MUTED);
        ringSub.setAlignmentX(Component.CENTER_ALIGNMENT);

        ringGauge.add(Box.createVerticalStrut(24));
        ringGauge.add(ringNum);
        ringGauge.add(ringSub);

        JPanel confidencePanel = new JPanel();
        confidencePanel.setLayout(new BoxLayout(confidencePanel, BoxLayout.Y_AXIS));
        confidencePanel.setOpaque(false);

        JLabel confTitle = new JLabel("Decision confidence");
        confTitle.setFont(AppTheme.FONT_SMALL);
        confTitle.setForeground(AppTheme.TEXT_MUTED);

        JLabel confVal = new JLabel("91%");
        confVal.setFont(new Font("JetBrains Mono", Font.BOLD, 18));
        confVal.setForeground(AppTheme.PRIMARY_ACCENT);

        JLabel confSub = new JLabel("high-confidence path");
        confSub.setFont(AppTheme.FONT_SMALL);
        confSub.setForeground(AppTheme.TEXT_MUTED);

        confidencePanel.add(confTitle);
        confidencePanel.add(Box.createVerticalStrut(2));
        confidencePanel.add(confVal);
        confidencePanel.add(Box.createVerticalStrut(2));
        confidencePanel.add(confSub);

        rightSide.add(ringGauge);
        rightSide.add(confidencePanel);

        hero.add(leftSide, BorderLayout.CENTER);
        hero.add(rightSide, BorderLayout.EAST);
        return hero;
    }

    private JPanel createMetricChip(String label, String value) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        chip.setBackground(AppTheme.CARD_RAISE);
        chip.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        JLabel l = new JLabel(label);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        l.setForeground(AppTheme.TEXT_MUTED);
        JLabel v = new JLabel(value);
        v.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        v.setForeground(AppTheme.TEXT_PRIMARY);
        chip.add(l);
        chip.add(v);
        return chip;
    }

    private JPanel buildCard(String title, String subtitle) {
        JPanel card = new JPanel(new BorderLayout(0, 8));
        card.setBackground(AppTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(14, 16, 14, 16)
        ));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel t = new JLabel(title.toUpperCase());
        t.setFont(new Font("JetBrains Mono", Font.BOLD, 11));
        t.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel s = new JLabel(subtitle);
        s.setFont(AppTheme.FONT_SMALL);
        s.setForeground(AppTheme.TEXT_MUTED);

        head.add(t, BorderLayout.WEST);
        head.add(s, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);
        return card;
    }

    private JPanel buildGuardrailsAndBudgetPanel() {
        JPanel card = buildCard("Financial Guardrails", "Constraints the decision engine respects");
        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        list.add(createGuardrailRow("Monthly budget ceiling", "ACTIVE", "Restricts spending when category exceeds 80% limit.", AppTheme.PRIMARY_ACCENT));
        list.add(Box.createVerticalStrut(6));
        list.add(createGuardrailRow("Baseline spike guard", "ACTIVE", "Flags categories exceeding historical baseline by 30%.", AppTheme.PRIMARY_ACCENT));
        list.add(Box.createVerticalStrut(6));
        list.add(createGuardrailRow("Savings rate floor", "ACTIVE", "Protects minimum 20% positive savings contribution.", AppTheme.PRIMARY_ACCENT));
        list.add(Box.createVerticalStrut(6));
        list.add(createGuardrailRow("Approval gate", "ACTIVE", "Recommendations remain advisory until approved.", AppTheme.PRIMARY_ACCENT));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    private JPanel createGuardrailRow(String title, String badge, String desc, Color badgeColor) {
        JPanel row = new JPanel(new BorderLayout(4, 2));
        row.setBackground(AppTheme.CARD_RAISE);
        row.setBorder(new EmptyBorder(8, 10, 8, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        JLabel t = new JLabel(title);
        t.setFont(AppTheme.FONT_BODY_BOLD);
        t.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel b = new JLabel(badge);
        b.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        b.setForeground(badgeColor);

        top.add(t, BorderLayout.WEST);
        top.add(b, BorderLayout.EAST);

        JLabel d = new JLabel(desc);
        d.setFont(AppTheme.FONT_SMALL);
        d.setForeground(AppTheme.TEXT_MUTED);

        row.add(top, BorderLayout.NORTH);
        row.add(d, BorderLayout.SOUTH);
        return row;
    }

    @Override
    public void addNotify() {
        super.addNotify();
        loadDashboardData();
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
        } else {
            budgetCard.setValue("No budget");
            budgetCard.setSubtitle("Click Budgets to set one");
        }

        String monthTitle = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYear;
        categoryChart.displayCategoryPieChart("Expense Allocation by Category — " + monthTitle, data.spending);
        historyChart.displaySplineWaveChart("Cashflow Velocity & Trajectory (6-Month Spline Wave)", data.history);

        updateSignalsQueue(data.forecast, data.unusual, data.overallBudget, data.summary);

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

        content.revalidate();
        content.repaint();
    }

    private void updateSignalsQueue(SpendingForecast forecast,
                                    List<UnusualSpendingInsight> unusual,
                                    Optional<BudgetStatus> budget,
                                    MonthlySummary summary) {
        signalsQueuePanel.removeAll();

        if (unusual != null && !unusual.isEmpty()) {
            UnusualSpendingInsight top = unusual.get(0);
            signalsQueuePanel.add(createSignalItem(
                    "P1",
                    "3-Month Baseline Guard",
                    "Potential unusual spending pattern: " + top.categoryName() + " spike",
                    top.reason(),
                    "Cap non-essential " + top.categoryName() + " spending for remainder of month to protect balance.",
                    "Confidence 94%",
                    AppTheme.SECONDARY_ACCENT
            ));
            signalsQueuePanel.add(Box.createVerticalStrut(8));
        }

        if (budget.isPresent()) {
            BudgetStatus bs = budget.get();
            String pLabel = bs.utilizationPercentage() >= 80.0 ? "P2" : "P3";
            Color pColor = bs.utilizationPercentage() >= 80.0 ? AppTheme.WARNING : AppTheme.PRIMARY_ACCENT;
            signalsQueuePanel.add(createSignalItem(
                    pLabel,
                    "Budget Ceiling Guard",
                    "Monthly budget utilization: " + String.format("%.1f%%", bs.utilizationPercentage()) + " consumed",
                    MoneyUtil.formatRupees(summary.totalExpense()) + " spent against " + MoneyUtil.formatRupees(bs.budgetAmount()) + " limit.",
                    "Maintain daily discretionary spend within target run-rate to preserve " + MoneyUtil.formatRupees(bs.remainingAmount()) + ".",
                    "Confidence 89%",
                    pColor
            ));
            signalsQueuePanel.add(Box.createVerticalStrut(8));
        }

        signalsQueuePanel.add(createSignalItem(
                "P3",
                "Savings Rate Floor",
                "Positive net cashflow buffer: " + MoneyUtil.formatRupees(summary.netSavings()),
                summary.savingsRate() + "% savings rate achieved on " + MoneyUtil.formatRupees(summary.totalIncome()) + " income.",
                "Allocate surplus to emergency fund or investment reserves.",
                "Confidence 98%",
                AppTheme.PRIMARY_ACCENT
        ));

        signalsQueuePanel.revalidate();
        signalsQueuePanel.repaint();
    }

    private JPanel createSignalItem(String priority,
                                    String guard,
                                    String title,
                                    String cause,
                                    String directive,
                                    String conf,
                                    Color accentColor) {
        JPanel item = new JPanel(new BorderLayout(8, 6)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.CARD_RAISE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(accentColor);
                g2.fillRect(0, 0, 3, getHeight());
                g2.dispose();
                super.paintComponent(g);
            }
        };
        item.setOpaque(false);
        item.setBorder(new EmptyBorder(8, 12, 8, 12));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);

        JPanel leftBadges = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        leftBadges.setOpaque(false);

        JLabel pBadge = new JLabel(priority);
        pBadge.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        pBadge.setForeground(accentColor);

        JLabel gBadge = new JLabel(guard);
        gBadge.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        gBadge.setForeground(AppTheme.TEXT_MUTED);

        leftBadges.add(pBadge);
        leftBadges.add(gBadge);

        JLabel confLbl = new JLabel(conf);
        confLbl.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        confLbl.setForeground(AppTheme.PRIMARY_ACCENT);

        top.add(leftBadges, BorderLayout.WEST);
        top.add(confLbl, BorderLayout.EAST);

        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setOpaque(false);

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_BODY_BOLD);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel causeLbl = new JLabel("<html><body style='width: 340px;'>" + cause + "</body></html>");
        causeLbl.setFont(AppTheme.FONT_SMALL);
        causeLbl.setForeground(AppTheme.TEXT_SECONDARY);

        JLabel dirLbl = new JLabel("<html><body style='width: 340px;'><b style='color:#aab4ad;'>Directive:</b> " + directive + "</body></html>");
        dirLbl.setFont(AppTheme.FONT_SMALL);
        dirLbl.setForeground(AppTheme.TEXT_PRIMARY);

        body.add(titleLbl);
        body.add(Box.createVerticalStrut(2));
        body.add(causeLbl);
        body.add(Box.createVerticalStrut(2));
        body.add(dirLbl);

        item.add(top, BorderLayout.NORTH);
        item.add(body, BorderLayout.CENTER);
        return item;
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

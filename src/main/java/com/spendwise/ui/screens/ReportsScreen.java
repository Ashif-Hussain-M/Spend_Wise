package com.spendwise.ui.screens;

import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthComparison;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.SimulatorResult;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.ChartPanel;
import com.spendwise.ui.components.ScrollablePanel;
import com.spendwise.ui.components.SectionHeader;
import com.spendwise.ui.components.SimulatorComparisonChart;
import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.components.StyledComboBox;
import com.spendwise.ui.components.StyledTable;
import com.spendwise.ui.components.StyledTextField;
import com.spendwise.ui.components.SummaryCard;
import com.spendwise.ui.components.Toast;
import com.spendwise.ui.theme.AppIcons;
import com.spendwise.ui.theme.AppTheme;
import com.spendwise.util.MoneyUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ReportsScreen extends JPanel implements BaseScreen {

    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;
    private final CategoryService categoryService;

    private int currentYear;
    private int currentMonth;

    private final ScrollablePanel content;
    private final SummaryCard incomeCard;
    private final SummaryCard expenseCard;
    private final SummaryCard savingsCard;
    private final SummaryCard rateCard;

    private final ChartPanel categoryChart;
    private final ChartPanel historyChart;

    private final DefaultTableModel breakdownModel;
    private final StyledTable breakdownTable;
    private final List<CategorySpending> currentBreakdown = new ArrayList<>();

    private final StyledComboBox<CategoryItem> simCategoryCombo;
    private final JSlider simSlider;
    private final StyledTextField simPercentField;
    private final JLabel simSavingsLabel;
    private final JLabel simBalanceLabel;
    private final SimulatorComparisonChart simChart;
    private final Map<Long, BigDecimal> categorySpendingsMap = new HashMap<>();
    private BigDecimal currentExpenseTotal = BigDecimal.ZERO;

    public ReportsScreen(ReportService reportService, SpendingAnalysisService analysisService, CategoryService categoryService) {
        this.reportService = reportService;
        this.analysisService = analysisService;
        this.categoryService = categoryService;

        this.currentYear = LocalDate.now().getYear();
        this.currentMonth = LocalDate.now().getMonthValue();

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 24, 0));

        SectionHeader header = new SectionHeader("Financial Analytics & Reports", "Aggregated insights, monthly comparisons, CSV export, and what-if simulator");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton exportBtn = new StyledButton("Export CSV", StyledButton.ButtonStyle.SECONDARY);
        exportBtn.addActionListener(e -> exportCsv());
        header.addAction(exportBtn);

        content.add(header);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 4, 16, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardsPanel.setPreferredSize(new Dimension(800, 115));
        cardsPanel.setMaximumSize(new Dimension(3000, 115));

        incomeCard = new SummaryCard("TOTAL INCOME", "Rs 0.00", "Selected Month", AppIcons.add(18, AppTheme.SUCCESS), AppTheme.SUCCESS);
        expenseCard = new SummaryCard("TOTAL EXPENSES", "Rs 0.00", "Selected Month", AppIcons.delete(18, AppTheme.DANGER), AppTheme.DANGER);
        savingsCard = new SummaryCard("NET SAVINGS", "Rs 0.00", "Net Balance", AppIcons.dashboard(18, AppTheme.BLUE_ACCENT), AppTheme.BLUE_ACCENT);
        rateCard = new SummaryCard("SAVINGS RATE", "0.0%", "Income retained", AppIcons.reports(18, AppTheme.TEAL_ACCENT), AppTheme.TEAL_ACCENT);

        cardsPanel.add(incomeCard);
        cardsPanel.add(expenseCard);
        cardsPanel.add(savingsCard);
        cardsPanel.add(rateCard);
        content.add(cardsPanel);

        content.add(Box.createVerticalStrut(20));

        JPanel chartsPanel = new JPanel(new GridLayout(1, 2, 16, 0));
        chartsPanel.setOpaque(false);
        chartsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        chartsPanel.setPreferredSize(new Dimension(800, 280));
        chartsPanel.setMaximumSize(new Dimension(3000, 280));

        categoryChart = new ChartPanel();
        historyChart = new ChartPanel();

        chartsPanel.add(categoryChart);
        chartsPanel.add(historyChart);
        content.add(chartsPanel);

        content.add(Box.createVerticalStrut(24));

        SectionHeader breakdownHeader = new SectionHeader("Spending Breakdown by Category", "Granular expenditure details for the selected period");
        breakdownHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(breakdownHeader);

        String[] cols = {"Category", "Type", "Transactions", "Total Amount", "Share of Spending"};
        breakdownModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        breakdownTable = new StyledTable(breakdownModel);
        JScrollPane tableScroll = new JScrollPane(breakdownTable);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setBackground(AppTheme.CARD_BG);
        tableScroll.getViewport().setBackground(AppTheme.CARD_BG);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(800, 180));
        tableScroll.setMaximumSize(new Dimension(3000, 220));
        content.add(tableScroll);

        content.add(Box.createVerticalStrut(24));

        SectionHeader simHeader = new SectionHeader("What-If Spending Simulator", "Interactive calculation of potential savings by reducing category expenses (pure calculation, never writes to DB)");
        simHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(simHeader);

        JPanel simCard = new JPanel(new BorderLayout(24, 16));
        simCard.setBackground(AppTheme.CARD_BG);
        simCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        simCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(18, 20, 18, 20)
        ));
        simCard.setPreferredSize(new Dimension(800, 240));
        simCard.setMaximumSize(new Dimension(3000, 260));

        JPanel leftPanel = new JPanel();
        leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
        leftPanel.setOpaque(false);

        JPanel row1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel targetLbl = new JLabel("Target Expense Category:");
        targetLbl.setFont(AppTheme.FONT_BODY_BOLD);
        targetLbl.setForeground(AppTheme.TEXT_SECONDARY);

        simCategoryCombo = new StyledComboBox<>();
        simCategoryCombo.setPreferredSize(new Dimension(200, 36));
        simCategoryCombo.addActionListener(e -> runSimulation());

        row1.add(targetLbl);
        row1.add(simCategoryCombo);

        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel redLbl = new JLabel("Spending Reduction:");
        redLbl.setFont(AppTheme.FONT_BODY_BOLD);
        redLbl.setForeground(AppTheme.TEXT_SECONDARY);

        simSlider = new JSlider(0, 100, 25);
        simSlider.setOpaque(false);
        simSlider.setPreferredSize(new Dimension(150, 32));

        simPercentField = new StyledTextField(4);
        simPercentField.setText("25");

        simSlider.addChangeListener(e -> {
            simPercentField.setText(String.valueOf(simSlider.getValue()));
            runSimulation();
        });

        simPercentField.addActionListener(e -> {
            try {
                int val = Integer.parseInt(simPercentField.getText().trim());
                if (val >= 0 && val <= 100) {
                    simSlider.setValue(val);
                }
            } catch (Exception ignored) {
            }
        });

        StyledButton simCalculateBtn = new StyledButton("Simulate", StyledButton.ButtonStyle.PRIMARY);
        simCalculateBtn.addActionListener(e -> {
            runSimulation();
            showToast("What-If simulation recalculated", Toast.ToastType.SUCCESS);
        });

        StyledButton simResetBtn = new StyledButton("Reset", StyledButton.ButtonStyle.GHOST);
        simResetBtn.addActionListener(e -> resetSimulation());

        row2.add(redLbl);
        row2.add(simSlider);
        row2.add(simPercentField);
        row2.add(new JLabel("%"));
        row2.add(simCalculateBtn);
        row2.add(simResetBtn);

        JPanel presetRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        presetRow.setOpaque(false);
        presetRow.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel presetLbl = new JLabel("Quick Presets:");
        presetLbl.setFont(AppTheme.FONT_SMALL);
        presetLbl.setForeground(AppTheme.TEXT_MUTED);
        presetRow.add(presetLbl);

        int[] presets = {10, 25, 50, 75};
        for (int p : presets) {
            StyledButton pBtn = new StyledButton(p + "%", StyledButton.ButtonStyle.SECONDARY);
            pBtn.setFont(AppTheme.FONT_SMALL);
            pBtn.setPreferredSize(new Dimension(56, 26));
            pBtn.addActionListener(ev -> simSlider.setValue(p));
            presetRow.add(pBtn);
        }

        JPanel row3 = new JPanel(new GridLayout(1, 2, 16, 0));
        row3.setOpaque(false);
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel savBox = new JPanel(new BorderLayout(4, 4));
        savBox.setBackground(new Color(16, 185, 129, 20));
        savBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(16, 185, 129, 80), 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel savTitle = new JLabel("POTENTIAL SAVINGS");
        savTitle.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        savTitle.setForeground(AppTheme.PRIMARY_ACCENT);
        simSavingsLabel = new JLabel("Rs 0.00");
        simSavingsLabel.setFont(AppTheme.FONT_HEADER);
        simSavingsLabel.setForeground(AppTheme.PRIMARY_ACCENT);
        savBox.add(savTitle, BorderLayout.NORTH);
        savBox.add(simSavingsLabel, BorderLayout.CENTER);

        JPanel balBox = new JPanel(new BorderLayout(4, 4));
        balBox.setBackground(new Color(39, 48, 59, 40));
        balBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));
        JLabel balTitle = new JLabel("PROJECTED MONTHLY EXPENSES");
        balTitle.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        balTitle.setForeground(AppTheme.TEXT_SECONDARY);
        simBalanceLabel = new JLabel("Rs 0.00");
        simBalanceLabel.setFont(AppTheme.FONT_HEADER);
        simBalanceLabel.setForeground(AppTheme.TEXT_PRIMARY);
        balBox.add(balTitle, BorderLayout.NORTH);
        balBox.add(simBalanceLabel, BorderLayout.CENTER);

        row3.add(savBox);
        row3.add(balBox);

        leftPanel.add(row1);
        leftPanel.add(Box.createVerticalStrut(10));
        leftPanel.add(row2);
        leftPanel.add(Box.createVerticalStrut(6));
        leftPanel.add(presetRow);
        leftPanel.add(Box.createVerticalStrut(12));
        leftPanel.add(row3);

        simChart = new SimulatorComparisonChart();
        simChart.setPreferredSize(new Dimension(320, 190));

        simCard.add(leftPanel, BorderLayout.CENTER);
        simCard.add(simChart, BorderLayout.EAST);

        content.add(simCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);

        loadReportData();
    }

    @Override
    public void addNotify() {
        super.addNotify();
        loadReportData();
    }

    @Override
    public void onScreenSelected() {
        loadReportData();
    }

    @Override
    public void onMonthChanged(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        loadReportData();
    }

    public void loadReportData() {
        AsyncWorker.execute(
                () -> {
                    MonthlySummary summary = reportService.getMonthlySummary(1L, currentYear, currentMonth);
                    List<CategorySpending> spending = reportService.getCategorySpending(1L, currentYear, currentMonth, CategoryType.EXPENSE);
                    List<MonthComparison> history = reportService.getMonthlyHistory(1L, 6, LocalDate.of(currentYear, currentMonth, 15));
                    return new ReportPayload(summary, spending, history);
                },
                this::applyReportData,
                err -> showToast("Error loading reports: " + err.getMessage(), Toast.ToastType.ERROR)
        );
    }

    private void applyReportData(ReportPayload payload) {
        incomeCard.setValue(MoneyUtil.formatRupees(payload.summary.totalIncome()));
        expenseCard.setValue(MoneyUtil.formatRupees(payload.summary.totalExpense()));
        savingsCard.setValue(MoneyUtil.formatRupees(payload.summary.netSavings()));
        rateCard.setValue(payload.summary.savingsRate() + "%");

        if (payload.summary.netSavings().signum() >= 0) {
            savingsCard.setValueColor(AppTheme.SUCCESS);
        } else {
            savingsCard.setValueColor(AppTheme.DANGER);
        }

        String monthTitle = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYear;
        categoryChart.displayCategoryPieChart("Expense Distribution — " + monthTitle, payload.spending);
        historyChart.displaySplineWaveChart("Cashflow Velocity & Trajectory (6-Month Spline Wave)", payload.history);

        currentBreakdown.clear();
        currentBreakdown.addAll(payload.spending);
        breakdownModel.setRowCount(0);

        categorySpendingsMap.clear();
        simCategoryCombo.removeAllItems();

        currentExpenseTotal = payload.summary.totalExpense();

        for (CategorySpending cs : payload.spending) {
            categorySpendingsMap.put(cs.categoryId(), cs.totalAmount());
            simCategoryCombo.addItem(new CategoryItem(cs.categoryId(), cs.categoryName()));
            breakdownModel.addRow(new Object[]{
                    cs.categoryName(),
                    cs.categoryType().name(),
                    cs.transactionCount(),
                    MoneyUtil.formatRupees(cs.totalAmount()),
                    String.format("%.1f%%", cs.percentageOfTotal())
            });
        }

        if (simCategoryCombo.getItemCount() > 0) {
            simCategoryCombo.setSelectedIndex(0);
            runSimulation();
        } else {
            resetSimulation();
        }

        content.revalidate();
        content.repaint();
    }

    private void runSimulation() {
        CategoryItem item = (CategoryItem) simCategoryCombo.getSelectedItem();
        if (item == null || item.categoryId == null) {
            simSavingsLabel.setText("Rs 0.00");
            simBalanceLabel.setText(MoneyUtil.formatRupees(currentExpenseTotal));
            simChart.reset(currentExpenseTotal.doubleValue());
            return;
        }

        double percent;
        try {
            percent = Double.parseDouble(simPercentField.getText().trim());
            if (percent < 0 || percent > 100) {
                return;
            }
        } catch (NumberFormatException e) {
            return;
        }

        Map<Long, Double> reductions = Map.of(item.categoryId, percent);
        SimulatorResult result = analysisService.simulateSavings(currentExpenseTotal, categorySpendingsMap, reductions);

        simSavingsLabel.setText("+" + MoneyUtil.formatRupees(result.totalPotentialSavings()));
        simBalanceLabel.setText(MoneyUtil.formatRupees(result.projectedNewExpense()));

        BigDecimal categorySpending = categorySpendingsMap.getOrDefault(item.categoryId, BigDecimal.ZERO);
        simChart.updateSimulation(item.categoryName(), categorySpending.doubleValue(), percent);
    }

    private void resetSimulation() {
        simSlider.setValue(25);
        simPercentField.setText("25");
        simSavingsLabel.setText("Rs 0.00");
        simBalanceLabel.setText(MoneyUtil.formatRupees(currentExpenseTotal));
        simChart.reset(currentExpenseTotal.doubleValue());
    }

    private void exportCsv() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Export Monthly Report to CSV");
        chooser.setSelectedFile(new File("spendwise_report_" + currentYear + "_" + currentMonth + ".csv"));
        chooser.setFileFilter(new FileNameExtensionFilter("CSV Spreadsheet (*.csv)", "csv"));

        int result = chooser.showSaveDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.getName().toLowerCase().endsWith(".csv")) {
                file = new File(file.getAbsolutePath() + ".csv");
            }
            File finalFile = file;
            AsyncWorker.execute(
                    () -> {
                        try (PrintWriter pw = new PrintWriter(new FileWriter(finalFile))) {
                            pw.println("SpendWise Financial Report");
                            pw.println("Month," + Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYear);
                            pw.println();
                            pw.println("Metric,Value");
                            pw.println("Total Income," + incomeCard.getValue());
                            pw.println("Total Expenses," + expenseCard.getValue());
                            pw.println("Net Savings," + savingsCard.getValue());
                            pw.println("Savings Rate," + rateCard.getValue());
                            pw.println();
                            pw.println("Category,Type,Transactions,Total Amount (Rs),Share (%)");
                            for (CategorySpending cs : currentBreakdown) {
                                pw.printf("%s,%s,%d,%.2f,%.1f%%%n",
                                        escapeCsv(cs.categoryName()),
                                        cs.categoryType().name(),
                                        cs.transactionCount(),
                                        cs.totalAmount(),
                                        cs.percentageOfTotal()
                                );
                            }
                        }
                        return true;
                    },
                    ok -> showToast("Report exported successfully to: " + finalFile.getName(), Toast.ToastType.SUCCESS),
                    err -> showToast("Export failed: " + err.getMessage(), Toast.ToastType.ERROR)
            );
        }
    }

    private String escapeCsv(String val) {
        if (val.contains(",") || val.contains("\"")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }

    private void showToast(String msg, Toast.ToastType type) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Toast.show(topFrame, msg, type);
    }

    private record ReportPayload(MonthlySummary summary, List<CategorySpending> spending, List<MonthComparison> history) {
    }

    private record CategoryItem(Long categoryId, String categoryName) {
        @Override
        public String toString() {
            return categoryName;
        }
    }
}

package com.spendwise.ui.screens;

import com.spendwise.model.Category;
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
import java.awt.Component;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
    private final StyledTextField simPercentField;
    private final JLabel simSavingsLabel;
    private final JLabel simBalanceLabel;
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

        content.add(Box.createVerticalStrut(20));

        SectionHeader breakdownHeader = new SectionHeader("Category Spending Breakdown", "SQL-aggregated breakdown by volume and percentage");
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

        JPanel simCard = new JPanel(new BorderLayout(16, 12));
        simCard.setBackground(AppTheme.CARD_BG);
        simCard.setAlignmentX(Component.LEFT_ALIGNMENT);
        simCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)
        ));
        simCard.setPreferredSize(new Dimension(800, 110));
        simCard.setMaximumSize(new Dimension(3000, 120));

        JPanel simControls = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        simControls.setOpaque(false);

        simCategoryCombo = new StyledComboBox<>();
        simCategoryCombo.setPreferredSize(new Dimension(200, 36));

        simPercentField = new StyledTextField("Reduction % (e.g. 25)", 10);
        simPercentField.setText("25");

        StyledButton simCalculateBtn = new StyledButton("Simulate", StyledButton.ButtonStyle.PRIMARY);
        simCalculateBtn.addActionListener(e -> runSimulation());

        StyledButton simResetBtn = new StyledButton("Reset", StyledButton.ButtonStyle.GHOST);
        simResetBtn.addActionListener(e -> resetSimulation());

        simControls.add(new JLabel("Target Category:"));
        simControls.add(simCategoryCombo);
        simControls.add(new JLabel("Reduction:"));
        simControls.add(simPercentField);
        simControls.add(simCalculateBtn);
        simControls.add(simResetBtn);

        simCard.add(simControls, BorderLayout.NORTH);

        JPanel simResults = new JPanel(new FlowLayout(FlowLayout.LEFT, 24, 0));
        simResults.setOpaque(false);

        simSavingsLabel = new JLabel("Potential Savings: Rs 0.00");
        simSavingsLabel.setFont(AppTheme.FONT_BODY_BOLD);
        simSavingsLabel.setForeground(AppTheme.PRIMARY_ACCENT);

        simBalanceLabel = new JLabel("Projected New Expense: Rs 0.00");
        simBalanceLabel.setFont(AppTheme.FONT_BODY_BOLD);
        simBalanceLabel.setForeground(AppTheme.TEXT_PRIMARY);

        simResults.add(simSavingsLabel);
        simResults.add(simBalanceLabel);
        simCard.add(simResults, BorderLayout.SOUTH);

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
        historyChart.displayIncomeVsExpenseBarChart("Income vs Expenses (6-Month Trend)", payload.history);

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

        resetSimulation();
        content.revalidate();
        content.repaint();
    }

    private void runSimulation() {
        CategoryItem item = (CategoryItem) simCategoryCombo.getSelectedItem();
        if (item == null || item.categoryId == null) {
            showToast("No expense categories available to simulate.", Toast.ToastType.INFO);
            return;
        }

        double percent;
        try {
            percent = Double.parseDouble(simPercentField.getText().trim());
            if (percent < 0 || percent > 100) {
                showToast("Please enter a reduction percentage between 0 and 100.", Toast.ToastType.INFO);
                return;
            }
        } catch (NumberFormatException e) {
            showToast("Invalid percentage value.", Toast.ToastType.INFO);
            return;
        }

        Map<Long, Double> reductions = Map.of(item.categoryId, percent);
        SimulatorResult result = analysisService.simulateSavings(currentExpenseTotal, categorySpendingsMap, reductions);

        simSavingsLabel.setText("Potential Savings: " + MoneyUtil.formatRupees(result.totalPotentialSavings()));
        simBalanceLabel.setText("Projected New Expense: " + MoneyUtil.formatRupees(result.projectedNewExpense()));
    }

    private void resetSimulation() {
        simSavingsLabel.setText("Potential Savings: Rs 0.00");
        simBalanceLabel.setText("Projected New Expense: " + MoneyUtil.formatRupees(currentExpenseTotal));
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

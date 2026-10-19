package com.spendwise.ui.screens;

import com.spendwise.model.Budget;
import com.spendwise.model.BudgetStatus;
import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.service.BudgetService;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.ConfirmationDialog;
import com.spendwise.ui.components.EmptyStatePanel;
import com.spendwise.ui.components.ProgressIndicator;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

public class BudgetsScreen extends JPanel implements BaseScreen {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final ReportService reportService;

    private int currentYear;
    private int currentMonth;

    private final SummaryCard overallBudgetCard;
    private final SummaryCard spentCard;
    private final SummaryCard remainingCard;

    private final DefaultTableModel tableModel;
    private final StyledTable table;
    private final List<BudgetStatus> currentStatuses = new ArrayList<>();

    public BudgetsScreen(BudgetService budgetService, CategoryService categoryService, ReportService reportService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.reportService = reportService;

        this.currentYear = LocalDate.now().getYear();
        this.currentMonth = LocalDate.now().getMonthValue();

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 16, 0));

        SectionHeader header = new SectionHeader("Budgets & Spending Limits", "Set monthly expense caps and monitor category-level allocations");

        StyledButton setOverallBtn = new StyledButton("+ Set Overall Budget", StyledButton.ButtonStyle.PRIMARY);
        setOverallBtn.addActionListener(e -> showOverallBudgetDialog());

        StyledButton setCatBtn = new StyledButton("+ Set Category Budget", StyledButton.ButtonStyle.SECONDARY);
        setCatBtn.addActionListener(e -> showCategoryBudgetDialog(null));

        header.addAction(setOverallBtn);
        header.addAction(setCatBtn);
        content.add(header);

        JPanel cardsPanel = new JPanel(new GridLayout(1, 3, 16, 0));
        cardsPanel.setOpaque(false);
        cardsPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 115));

        overallBudgetCard = new SummaryCard("OVERALL BUDGET", "Rs 0.00", "Monthly allocation", AppIcons.budgets(18, AppTheme.PRIMARY_ACCENT), AppTheme.PRIMARY_ACCENT);
        spentCard = new SummaryCard("ACTUAL SPENT", "Rs 0.00", "Total expenses to date", AppIcons.delete(18, AppTheme.DANGER), AppTheme.DANGER);
        remainingCard = new SummaryCard("REMAINING BUDGET", "Rs 0.00", "Available limit", AppIcons.dashboard(18, AppTheme.BLUE_ACCENT), AppTheme.BLUE_ACCENT);

        cardsPanel.add(overallBudgetCard);
        cardsPanel.add(spentCard);
        cardsPanel.add(remainingCard);
        content.add(cardsPanel);

        content.add(Box.createVerticalStrut(20));

        SectionHeader listHeader = new SectionHeader("Category Budget Allocations", "Utilization progress and status alerts");
        content.add(listHeader);

        String[] cols = {"ID", "Category", "Budget Amount", "Actual Spent", "Remaining", "Utilization", "Status"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        table = new StyledTable(tableModel);
        table.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setBackground(AppTheme.CARD_BG);
        tableScroll.getViewport().setBackground(AppTheme.CARD_BG);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        content.add(tableScroll);

        content.add(Box.createVerticalStrut(12));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        StyledButton editBtn = new StyledButton("Edit Budget", StyledButton.ButtonStyle.SECONDARY);
        editBtn.addActionListener(e -> onEditSelected());

        StyledButton deleteBtn = new StyledButton("Delete Budget", StyledButton.ButtonStyle.DANGER);
        deleteBtn.addActionListener(e -> onDeleteSelected());

        actionRow.add(editBtn);
        actionRow.add(deleteBtn);
        content.add(actionRow);

        add(content, BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
        loadBudgets();
    }

    @Override
    public void onMonthChanged(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        loadBudgets();
    }

    public void loadBudgets() {
        AsyncWorker.execute(
                () -> {
                    Optional<BudgetStatus> overall = reportService.getOverallBudgetStatus(1L, currentYear, currentMonth);
                    List<BudgetStatus> catStatuses = reportService.getCategoryBudgetStatuses(1L, currentYear, currentMonth);
                    return new BudgetPayload(overall, catStatuses);
                },
                this::applyBudgets,
                err -> showToast("Error loading budgets: " + err.getMessage(), Toast.ToastType.ERROR)
        );
    }

    private void applyBudgets(BudgetPayload payload) {
        if (payload.overall.isPresent()) {
            BudgetStatus bs = payload.overall.get();
            overallBudgetCard.setValue(MoneyUtil.formatRupees(bs.budgetAmount()));
            spentCard.setValue(MoneyUtil.formatRupees(bs.actualSpent()));
            remainingCard.setValue(MoneyUtil.formatRupees(bs.remainingAmount()));
            remainingCard.setSubtitle(String.format("%.1f%% used (%s)", bs.utilizationPercentage(), bs.statusLabel()));
            if (bs.remainingAmount().signum() < 0) {
                remainingCard.setValueColor(AppTheme.DANGER);
            } else {
                remainingCard.setValueColor(AppTheme.SUCCESS);
            }
        } else {
            overallBudgetCard.setValue("No budget");
            spentCard.setValue(MoneyUtil.formatRupees(BigDecimal.ZERO));
            remainingCard.setValue("Rs 0.00");
            remainingCard.setSubtitle("No overall limit set");
        }

        currentStatuses.clear();
        currentStatuses.addAll(payload.categoryStatuses);
        tableModel.setRowCount(0);

        for (BudgetStatus bs : payload.categoryStatuses) {
            tableModel.addRow(new Object[]{
                    bs.budgetId(),
                    bs.categoryName(),
                    MoneyUtil.formatRupees(bs.budgetAmount()),
                    MoneyUtil.formatRupees(bs.actualSpent()),
                    MoneyUtil.formatRupees(bs.remainingAmount()),
                    String.format("%.1f%%", bs.utilizationPercentage()),
                    bs.statusLabel()
            });
        }
    }

    private void onEditSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showToast("Please select a category budget to edit.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);

        BudgetStatus target = currentStatuses.stream()
                .filter(b -> b.budgetId().equals(id))
                .findFirst()
                .orElse(null);

        if (target != null) {
            showCategoryBudgetDialog(target);
        }
    }

    private void onDeleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showToast("Please select a category budget to delete.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        String catName = (String) tableModel.getValueAt(modelRow, 1);

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.show(
                topFrame,
                "Delete Budget",
                "Are you sure you want to delete the budget for '" + catName + "'?",
                "Delete",
                true
        );

        if (confirmed) {
            AsyncWorker.execute(
                    () -> budgetService.deleteBudget(id),
                    success -> {
                        showToast("Budget deleted successfully.", Toast.ToastType.SUCCESS);
                        loadBudgets();
                    },
                    err -> showToast("Delete failed: " + err.getMessage(), Toast.ToastType.ERROR)
            );
        }
    }

    private void showOverallBudgetDialog() {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(topFrame, "Set Overall Monthly Budget", true);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);

        JPanel form = new JPanel(new GridLayout(2, 2, 12, 14));
        form.setBackground(AppTheme.CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));

        String monthName = Month.of(currentMonth).getDisplayName(TextStyle.FULL, Locale.ENGLISH) + " " + currentYear;
        StyledTextField amountInput = new StyledTextField(10);
        JLabel validationMsg = new JLabel("");
        validationMsg.setFont(AppTheme.FONT_SMALL);
        validationMsg.setForeground(AppTheme.DANGER);

        form.add(new JLabel("Month:"));
        form.add(new JLabel(monthName));
        form.add(new JLabel("Overall Budget (Rs):"));
        form.add(amountInput);

        dlg.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(AppTheme.CARD_BG);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.CARD_BORDER));

        StyledButton cancelBtn = new StyledButton("Cancel", StyledButton.ButtonStyle.SECONDARY);
        cancelBtn.addActionListener(e -> dlg.dispose());

        StyledButton saveBtn = new StyledButton("Save Budget", StyledButton.ButtonStyle.PRIMARY);
        saveBtn.addActionListener(e -> {
            try {
                BigDecimal amt = MoneyUtil.parse(amountInput.getText());
                AsyncWorker.execute(
                        () -> budgetService.setOverallBudget(1L, currentYear, currentMonth, amt),
                        saved -> {
                            dlg.dispose();
                            showToast("Overall budget set successfully.", Toast.ToastType.SUCCESS);
                            loadBudgets();
                        },
                        err -> validationMsg.setText(err.getMessage())
                );
            } catch (Exception ex) {
                validationMsg.setText(ex.getMessage());
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dlg.add(buttonPanel, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setLocationRelativeTo(topFrame);
        dlg.setVisible(true);
    }

    private void showCategoryBudgetDialog(BudgetStatus existing) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(topFrame, existing == null ? "Set Category Budget" : "Edit Budget for " + existing.categoryName(), true);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);

        JPanel form = new JPanel(new GridLayout(3, 2, 12, 14));
        form.setBackground(AppTheme.CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));

        StyledComboBox<CategoryItem> catCombo = new StyledComboBox<>();
        StyledTextField amountInput = new StyledTextField(10);
        JLabel validationMsg = new JLabel("");
        validationMsg.setFont(AppTheme.FONT_SMALL);
        validationMsg.setForeground(AppTheme.DANGER);

        AsyncWorker.execute(
                () -> categoryService.getCategoriesByType(1L, CategoryType.EXPENSE),
                cats -> {
                    catCombo.removeAllItems();
                    for (Category c : cats) {
                        catCombo.addItem(new CategoryItem(c.getCategoryId(), c.getCategoryName()));
                    }
                    if (existing != null) {
                        for (int i = 0; i < catCombo.getItemCount(); i++) {
                            if (catCombo.getItemAt(i).categoryId.equals(existing.categoryId())) {
                                catCombo.setSelectedIndex(i);
                                catCombo.setEnabled(false);
                                break;
                            }
                        }
                    }
                },
                null
        );

        if (existing != null) {
            amountInput.setText(existing.budgetAmount().toPlainString());
        }

        form.add(new JLabel("Expense Category:"));
        form.add(catCombo);
        form.add(new JLabel("Budget Amount (Rs):"));
        form.add(amountInput);
        form.add(new JLabel(""));
        form.add(validationMsg);

        dlg.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(AppTheme.CARD_BG);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.CARD_BORDER));

        StyledButton cancelBtn = new StyledButton("Cancel", StyledButton.ButtonStyle.SECONDARY);
        cancelBtn.addActionListener(e -> dlg.dispose());

        StyledButton saveBtn = new StyledButton("Save Budget", StyledButton.ButtonStyle.PRIMARY);
        saveBtn.addActionListener(e -> {
            try {
                CategoryItem item = (CategoryItem) catCombo.getSelectedItem();
                if (item == null || item.categoryId == null) {
                    validationMsg.setText("Please select a category.");
                    return;
                }
                BigDecimal amt = MoneyUtil.parse(amountInput.getText());
                AsyncWorker.execute(
                        () -> budgetService.setCategoryBudget(1L, item.categoryId, currentYear, currentMonth, amt),
                        saved -> {
                            dlg.dispose();
                            showToast("Category budget saved successfully.", Toast.ToastType.SUCCESS);
                            loadBudgets();
                        },
                        err -> validationMsg.setText(err.getMessage())
                );
            } catch (Exception ex) {
                validationMsg.setText(ex.getMessage());
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dlg.add(buttonPanel, BorderLayout.SOUTH);

        dlg.pack();
        dlg.setLocationRelativeTo(topFrame);
        dlg.setVisible(true);
    }

    private void showToast(String msg, Toast.ToastType type) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Toast.show(topFrame, msg, type);
    }

    private record BudgetPayload(Optional<BudgetStatus> overall, List<BudgetStatus> categoryStatuses) {
    }

    private record CategoryItem(Long categoryId, String categoryName) {
        @Override
        public String toString() {
            return categoryName;
        }
    }
}

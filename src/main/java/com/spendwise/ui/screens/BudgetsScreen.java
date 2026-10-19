package com.spendwise.ui.screens;

import com.spendwise.service.BudgetService;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class BudgetsScreen extends JPanel implements BaseScreen {

    private final BudgetService budgetService;
    private final CategoryService categoryService;
    private final ReportService reportService;

    public BudgetsScreen(BudgetService budgetService, CategoryService categoryService, ReportService reportService) {
        this.budgetService = budgetService;
        this.categoryService = categoryService;
        this.reportService = reportService;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);
        add(new JLabel("Budgets Screen", JLabel.CENTER), BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
    }

    @Override
    public void onMonthChanged(int year, int month) {
    }
}

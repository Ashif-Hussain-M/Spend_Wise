package com.spendwise.ui.screens;

import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class ReportsScreen extends JPanel implements BaseScreen {

    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;
    private final CategoryService categoryService;

    public ReportsScreen(ReportService reportService, SpendingAnalysisService analysisService, CategoryService categoryService) {
        this.reportService = reportService;
        this.analysisService = analysisService;
        this.categoryService = categoryService;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);
        add(new JLabel("Reports Screen", JLabel.CENTER), BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
    }

    @Override
    public void onMonthChanged(int year, int month) {
    }
}

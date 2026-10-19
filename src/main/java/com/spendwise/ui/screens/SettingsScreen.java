package com.spendwise.ui.screens;

import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class SettingsScreen extends JPanel implements BaseScreen {

    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;

    public SettingsScreen(ReportService reportService, SpendingAnalysisService analysisService) {
        this.reportService = reportService;
        this.analysisService = analysisService;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);
        add(new JLabel("Settings Screen", JLabel.CENTER), BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
    }

    @Override
    public void onMonthChanged(int year, int month) {
    }
}

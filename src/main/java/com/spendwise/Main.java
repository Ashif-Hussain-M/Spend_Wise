package com.spendwise;

import com.spendwise.repository.BudgetRepository;
import com.spendwise.repository.CategoryRepository;
import com.spendwise.repository.JdbcBudgetRepository;
import com.spendwise.repository.JdbcCategoryRepository;
import com.spendwise.repository.JdbcReportRepository;
import com.spendwise.repository.JdbcTransactionRepository;
import com.spendwise.repository.ReportRepository;
import com.spendwise.repository.TransactionRepository;
import com.spendwise.service.BudgetService;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.service.TransactionService;
import com.spendwise.ui.MainFrame;
import com.spendwise.ui.screens.BudgetsScreen;
import com.spendwise.ui.screens.CategoriesScreen;
import com.spendwise.ui.screens.DashboardScreen;
import com.spendwise.server.SpendWiseWebServer;
import com.spendwise.ui.screens.ReportsScreen;
import com.spendwise.ui.screens.SettingsScreen;
import com.spendwise.ui.screens.TransactionsScreen;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.SwingUtilities;

public class Main {

    public static void main(String[] args) {
        CategoryRepository categoryRepo = new JdbcCategoryRepository();
        TransactionRepository transactionRepo = new JdbcTransactionRepository();
        BudgetRepository budgetRepo = new JdbcBudgetRepository();
        ReportRepository reportRepo = new JdbcReportRepository();

        CategoryService categoryService = new CategoryService(categoryRepo);
        TransactionService transactionService = new TransactionService(transactionRepo, categoryRepo);
        BudgetService budgetService = new BudgetService(budgetRepo, categoryRepo);
        ReportService reportService = new ReportService(reportRepo);
        SpendingAnalysisService analysisService = new SpendingAnalysisService(reportRepo, categoryRepo);

        SpendWiseWebServer webServer = new SpendWiseWebServer(8080, categoryService, transactionService, budgetService, reportService, analysisService);
        webServer.start();

        SwingUtilities.invokeLater(() -> {
            AppTheme.setup();

            MainFrame frame = new MainFrame(categoryService, transactionService, budgetService, reportService, analysisService);

            DashboardScreen dashboardScreen = new DashboardScreen(reportService, analysisService, transactionService);
            TransactionsScreen transactionsScreen = new TransactionsScreen(transactionService, categoryService);
            CategoriesScreen categoriesScreen = new CategoriesScreen(categoryService);
            BudgetsScreen budgetsScreen = new BudgetsScreen(budgetService, categoryService, reportService);
            ReportsScreen reportsScreen = new ReportsScreen(reportService, analysisService, categoryService);
            SettingsScreen settingsScreen = new SettingsScreen(reportService, analysisService);

            frame.registerScreen("DASHBOARD", "Dashboard", dashboardScreen);
            frame.registerScreen("TRANSACTIONS", "Transactions", transactionsScreen);
            frame.registerScreen("CATEGORIES", "Category Management", categoriesScreen);
            frame.registerScreen("BUDGETS", "Budgets & Spending Limits", budgetsScreen);
            frame.registerScreen("REPORTS", "Financial Reports & Insights", reportsScreen);
            frame.registerScreen("SETTINGS", "Preferences & Diagnostics", settingsScreen);

            frame.setVisible(true);
        });
    }
}

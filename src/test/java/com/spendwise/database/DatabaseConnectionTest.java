package com.spendwise.database;

import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DatabaseConnectionTest {

    @Test
    void testConnectivity() {
        boolean connected = DatabaseConnection.checkConnectivity();
        assertTrue(connected);
    }

    @Test
    void testGetConnection() throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn);
            assertTrue(conn.isValid(2));
        }
    }

    @Test
    void testDashboardLoad() {
        com.spendwise.repository.JdbcCategoryRepository catRepo = new com.spendwise.repository.JdbcCategoryRepository();
        com.spendwise.repository.JdbcReportRepository reportRepo = new com.spendwise.repository.JdbcReportRepository();
        com.spendwise.repository.JdbcTransactionRepository txRepo = new com.spendwise.repository.JdbcTransactionRepository();
        com.spendwise.service.ReportService reportService = new com.spendwise.service.ReportService(reportRepo);
        com.spendwise.service.SpendingAnalysisService analysisService = new com.spendwise.service.SpendingAnalysisService(reportRepo, catRepo);
        com.spendwise.service.TransactionService txService = new com.spendwise.service.TransactionService(txRepo, catRepo);

        int currentYear = 2026;
        int currentMonth = 10;
        var summary = reportService.getMonthlySummary(1L, currentYear, currentMonth);
        java.time.YearMonth prevYm = java.time.YearMonth.of(currentYear, currentMonth).minusMonths(1);
        if (reportService.hasDataInMonth(1L, prevYm.getYear(), prevYm.getMonthValue())) {
            var prevSummary = reportService.getMonthlySummary(1L, prevYm.getYear(), prevYm.getMonthValue());
        }
        var overallBudget = reportService.getOverallBudgetStatus(1L, currentYear, currentMonth);
        var spending = reportService.getCategorySpending(1L, currentYear, currentMonth, com.spendwise.model.CategoryType.EXPENSE);
        var history = reportService.getMonthlyHistory(1L, 6, java.time.LocalDate.of(currentYear, currentMonth, 15));
        java.time.LocalDate forecastDate = java.time.LocalDate.of(currentYear, currentMonth, 19);
        var forecast = analysisService.forecastMonthlySpending(1L, currentYear, currentMonth, forecastDate);
        var unusual = analysisService.detectUnusualSpending(1L, forecastDate);
        var recent = txService.getRecentTransactions(1L, 6);

        assertNotNull(summary);
        assertNotNull(forecast);
        assertNotNull(unusual);
        assertNotNull(recent);
    }
}

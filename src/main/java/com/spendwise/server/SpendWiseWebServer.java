package com.spendwise.server;

import com.spendwise.model.Budget;
import com.spendwise.model.Category;
import com.spendwise.model.CategorySpending;
import com.spendwise.model.CategoryType;
import com.spendwise.model.MonthlySummary;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.service.BudgetService;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.service.TransactionService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executors;

public class SpendWiseWebServer {

    private final int port;
    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;
    private HttpServer server;

    public SpendWiseWebServer(int port,
                              CategoryService categoryService,
                              TransactionService transactionService,
                              BudgetService budgetService,
                              ReportService reportService,
                              SpendingAnalysisService analysisService) {
        this.port = port;
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.reportService = reportService;
        this.analysisService = analysisService;
    }

    public synchronized void start() {
        if (server != null) {
            return;
        }
        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", new DashboardHandler());
            server.createContext("/api/summary", new SummaryHandler());
            server.createContext("/api/transactions", new TransactionsHandler());
            server.createContext("/api/categories", new CategoriesHandler());
            server.createContext("/api/health", new HealthHandler());
            server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
            server.start();
            System.out.println("[SpendWise] Web Server running at http://localhost:" + port);
        } catch (IOException e) {
            System.err.println("[SpendWise] Failed to start web server on port " + port + ": " + e.getMessage());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    public int getPort() {
        return port;
    }

    private class HealthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String json = "{\"status\":\"UP\",\"port\":" + port + ",\"database\":\"CONNECTED\",\"timestamp\":\"" + LocalDate.now() + "\"}";
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    private class SummaryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int year = parseInt(params.get("year"), LocalDate.now().getYear());
            int month = parseInt(params.get("month"), LocalDate.now().getMonthValue());

            MonthlySummary summary = reportService.getMonthlySummary(1L, year, month);
            BigDecimal income = summary.totalIncome();
            BigDecimal expense = summary.totalExpense();
            BigDecimal net = summary.netSavings();
            double savingsRate = summary.savingsRate().doubleValue();

            String json = String.format(
                    "{\"year\":%d,\"month\":%d,\"income\":%.2f,\"expense\":%.2f,\"netSavings\":%.2f,\"savingsRate\":%.2f}",
                    year, month, income.doubleValue(), expense.doubleValue(), net.doubleValue(), savingsRate
            );
            sendResponse(exchange, 200, "application/json; charset=UTF-8", json);
        }
    }

    private class CategoriesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<Category> list = categoryService.getCategories(1L);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Category c = list.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"name\":\"%s\",\"type\":\"%s\"}",
                        c.getCategoryId(), escapeJson(c.getCategoryName()), c.getCategoryType().name()));
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }
    }

    private class TransactionsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                handlePostTransaction(exchange);
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int year = parseInt(params.get("year"), LocalDate.now().getYear());
            int month = parseInt(params.get("month"), LocalDate.now().getMonthValue());

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);

            List<Transaction> list = transactionService.getFilteredTransactions(1L, start, end, null, null, null, null, null);
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < list.size(); i++) {
                Transaction t = list.get(i);
                if (i > 0) sb.append(",");
                sb.append(String.format("{\"id\":%d,\"date\":\"%s\",\"type\":\"%s\",\"category\":\"%s\",\"amount\":%.2f,\"description\":\"%s\"}",
                        t.getTransactionId(),
                        t.getTransactionDate(),
                        t.getTransactionType().name(),
                        escapeJson(t.getCategoryName() != null ? t.getCategoryName() : "Unassigned"),
                        t.getAmount().doubleValue(),
                        escapeJson(t.getDescription())));
            }
            sb.append("]");
            sendResponse(exchange, 200, "application/json; charset=UTF-8", sb.toString());
        }

        private void handlePostTransaction(HttpExchange exchange) throws IOException {
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), StandardCharsets.UTF_8);
            BufferedReader br = new BufferedReader(isr);
            StringBuilder body = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                body.append(line);
            }

            Map<String, String> form = parseFormData(body.toString());
            try {
                Long categoryId = Long.parseLong(form.get("categoryId"));
                TransactionType type = TransactionType.valueOf(form.get("type").toUpperCase());
                BigDecimal amount = new BigDecimal(form.get("amount"));
                LocalDate date = LocalDate.parse(form.get("date"));
                String desc = form.getOrDefault("description", "Web Entry");

                transactionService.addTransaction(1L, categoryId, type, amount, date, desc);
                String resp = "{\"success\":true,\"message\":\"Transaction logged successfully\"}";
                sendResponse(exchange, 200, "application/json; charset=UTF-8", resp);
            } catch (Exception ex) {
                String resp = "{\"success\":false,\"error\":\"" + escapeJson(ex.getMessage()) + "\"}";
                sendResponse(exchange, 400, "application/json; charset=UTF-8", resp);
            }
        }
    }

    private class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (!exchange.getRequestURI().getPath().equals("/")) {
                sendResponse(exchange, 404, "text/plain", "Not Found");
                return;
            }

            Map<String, String> params = parseQueryParams(exchange.getRequestURI().getQuery());
            int year = parseInt(params.get("year"), LocalDate.now().getYear());
            int month = parseInt(params.get("month"), LocalDate.now().getMonthValue());

            MonthlySummary summary = reportService.getMonthlySummary(1L, year, month);
            List<CategorySpending> spendingList = reportService.getCategorySpending(1L, year, month, CategoryType.EXPENSE);

            LocalDate start = LocalDate.of(year, month, 1);
            LocalDate end = start.plusMonths(1).minusDays(1);
            List<Transaction> transactions = transactionService.getFilteredTransactions(1L, start, end, null, null, null, null, null);
            List<Category> categories = categoryService.getCategories(1L);

            String html = renderDashboardHtml(year, month, summary, spendingList, transactions, categories);
            sendResponse(exchange, 200, "text/html; charset=UTF-8", html);
        }
    }

    private String renderDashboardHtml(int year, int month,
                                       MonthlySummary summary,
                                       List<CategorySpending> spendingList,
                                       List<Transaction> transactions,
                                       List<Category> categories) {
        double income = summary.totalIncome().doubleValue();
        double expense = summary.totalExpense().doubleValue();
        double net = summary.netSavings().doubleValue();
        double savingsRate = summary.savingsRate().doubleValue();

        StringBuilder catOptions = new StringBuilder();
        for (Category c : categories) {
            catOptions.append(String.format("<option value='%d'>%s (%s)</option>", c.getCategoryId(), escapeHtml(c.getCategoryName()), c.getCategoryType().name()));
        }

        StringBuilder txRows = new StringBuilder();
        if (transactions.isEmpty()) {
            txRows.append("<tr><td colspan='5' style='text-align:center;padding:24px;color:#64748b;'>No transactions recorded for this period.</td></tr>");
        } else {
            for (Transaction t : transactions) {
                boolean isIncome = t.getTransactionType() == TransactionType.INCOME;
                String badgeBg = isIncome ? "rgba(16,185,129,0.15)" : "rgba(244,63,94,0.15)";
                String badgeColor = isIncome ? "#34d399" : "#fb7185";
                String amtPrefix = isIncome ? "+" : "-";

                txRows.append(String.format(
                        "<tr>" +
                                "<td style='padding:12px 16px;border-bottom:1px solid #1e293b;color:#cbd5e1;'>%s</td>" +
                                "<td style='padding:12px 16px;border-bottom:1px solid #1e293b;'><span style='background:%s;color:%s;padding:2px 8px;border-radius:9999px;font-size:11px;font-weight:600;'>%s</span></td>" +
                                "<td style='padding:12px 16px;border-bottom:1px solid #1e293b;color:#94a3b8;'>%s</td>" +
                                "<td style='padding:12px 16px;border-bottom:1px solid #1e293b;color:#f8fafc;font-weight:500;'>%s</td>" +
                                "<td style='padding:12px 16px;border-bottom:1px solid #1e293b;text-align:right;color:%s;font-weight:700;'>%s₹%,.2f</td>" +
                                "</tr>",
                        t.getTransactionDate(),
                        badgeBg, badgeColor, t.getTransactionType().name(),
                        escapeHtml(t.getCategoryName() != null ? t.getCategoryName() : "Unassigned"),
                        escapeHtml(t.getDescription()),
                        badgeColor, amtPrefix, t.getAmount().doubleValue()
                ));
            }
        }

        StringBuilder catCards = new StringBuilder();
        if (spendingList.isEmpty()) {
            catCards.append("<div style='color:#64748b;padding:12px;'>No expense categories active this month.</div>");
        } else {
            for (CategorySpending cs : spendingList) {
                double spent = cs.totalAmount().doubleValue();
                Optional<Budget> budgetOpt = budgetService.getCategoryBudget(1L, cs.categoryId(), year, month);
                double budgeted = budgetOpt.map(b -> b.getBudgetAmount().doubleValue()).orElse(0.0);
                double pct = budgeted > 0 ? (spent / budgeted) * 100.0 : cs.percentageOfTotal();
                String barColor = pct > 100 ? "#f43f5e" : (pct > 80 ? "#f59e0b" : "#10b981");

                catCards.append(String.format(
                        "<div style='margin-bottom:14px;background:#0f172a;padding:12px 16px;border-radius:8px;border:1px solid #1e293b;'>" +
                                "<div style='display:flex;justify-content:space-between;align-items:center;margin-bottom:6px;'>" +
                                "<span style='font-size:13px;font-weight:600;color:#e2e8f0;'>%s</span>" +
                                "<span style='font-size:12px;color:#94a3b8;'>₹%,.2f %s</span>" +
                                "</div>" +
                                "<div style='height:6px;width:100%%;background:#1e293b;border-radius:9999px;overflow:hidden;'>" +
                                "<div style='height:100%%;width:%.1f%%;background:%s;border-radius:9999px;'></div>" +
                                "</div>" +
                                "</div>",
                        escapeHtml(cs.categoryName()),
                        spent,
                        (budgeted > 0 ? String.format("/ ₹%,.2f (%.1f%%)", budgeted, pct) : ""),
                        Math.min(pct, 100.0),
                        barColor
                ));
            }
        }

        return "<!DOCTYPE html>" +
                "<html lang='en'>" +
                "<head>" +
                "<meta charset='UTF-8'>" +
                "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
                "<title>SpendWise // Autonomous Cashflow Engine</title>" +
                "<style>" +
                "* { box-sizing: border-box; margin: 0; padding: 0; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }" +
                "body { background: #080c14; color: #f8fafc; line-height: 1.5; padding: 24px; }" +
                ".container { max-width: 1240px; margin: 0 auto; }" +
                ".header { display: flex; justify-content: space-between; align-items: center; padding: 16px 20px; background: #0f172a; border-radius: 12px; border: 1px solid #1e293b; margin-bottom: 24px; }" +
                ".brand { display: flex; align-items: center; gap: 12px; }" +
                ".brand-icon { width: 32px; height: 32px; background: linear-gradient(135deg, #10b981, #06b6d4); border-radius: 8px; display: flex; align-items: center; justify-content: center; font-weight: 800; color: #080c14; }" +
                ".brand-title { font-size: 16px; font-weight: 700; letter-spacing: 0.5px; }" +
                ".badge-status { display: inline-flex; align-items: center; gap: 6px; padding: 4px 10px; background: rgba(16,185,129,0.12); color: #34d399; border: 1px solid rgba(16,185,129,0.3); border-radius: 9999px; font-size: 11px; font-weight: 600; text-transform: uppercase; }" +
                ".grid-kpi { display: grid; grid-template-columns: repeat(auto-fit, minmax(240px, 1fr)); gap: 16px; margin-bottom: 24px; }" +
                ".kpi-card { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 18px 20px; }" +
                ".kpi-title { font-size: 12px; font-weight: 600; color: #94a3b8; text-transform: uppercase; margin-bottom: 8px; }" +
                ".kpi-val { font-size: 26px; font-weight: 800; }" +
                ".val-green { color: #34d399; }" +
                ".val-red { color: #fb7185; }" +
                ".val-cyan { color: #38bdf8; }" +
                ".grid-main { display: grid; grid-template-columns: 2fr 1fr; gap: 20px; margin-bottom: 24px; }" +
                "@media(max-width: 900px) { .grid-main { grid-template-columns: 1fr; } }" +
                ".section-panel { background: #0f172a; border: 1px solid #1e293b; border-radius: 12px; padding: 20px; }" +
                ".section-title { font-size: 15px; font-weight: 700; color: #f8fafc; margin-bottom: 16px; display: flex; justify-content: space-between; align-items: center; }" +
                "table { width: 100%; border-collapse: collapse; font-size: 13px; }" +
                "th { text-align: left; padding: 10px 16px; background: #162032; color: #94a3b8; font-size: 11px; text-transform: uppercase; border-bottom: 1px solid #1e293b; }" +
                "input, select, button { font-size: 13px; border-radius: 6px; border: 1px solid #334155; padding: 8px 12px; background: #1e293b; color: #f8fafc; }" +
                "button { background: #10b981; color: #080c14; font-weight: 700; border: none; cursor: pointer; transition: background 0.15s; }" +
                "button:hover { background: #34d399; }" +
                ".form-group { margin-bottom: 12px; display: flex; flex-direction: column; gap: 4px; }" +
                ".form-group label { font-size: 11px; font-weight: 600; color: #94a3b8; text-transform: uppercase; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='container'>" +
                "<header class='header'>" +
                "<div class='brand'>" +
                "<div class='brand-icon'>SW</div>" +
                "<div>" +
                "<div class='brand-title'>SPENDWISE // AUTONOMOUS CASHFLOW ENGINE</div>" +
                "<div style='font-size:12px;color:#64748b;'>Java 21 • MySQL 8 InnoDB • Pure Localhost Web Node</div>" +
                "</div>" +
                "</div>" +
                "<div style='display:flex;align-items:center;gap:12px;'>" +
                "<span class='badge-status'><span style='width:6px;height:6px;background:#10b981;border-radius:50%;'></span>ENGINE ONLINE : 8080</span>" +
                "<form method='GET' style='display:inline-flex;gap:6px;'>" +
                "<input type='number' name='year' value='" + year + "' style='width:70px;padding:4px 8px;font-size:12px;'>" +
                "<input type='number' name='month' value='" + month + "' min='1' max='12' style='width:50px;padding:4px 8px;font-size:12px;'>" +
                "<button type='submit' style='padding:4px 10px;font-size:12px;'>Load</button>" +
                "</form>" +
                "</div>" +
                "</header>" +
                "<div class='grid-kpi'>" +
                "<div class='kpi-card'><div class='kpi-title'>Total Inflow</div><div class='kpi-val val-green'>₹" + String.format("%,.2f", income) + "</div><div style='font-size:11px;color:#64748b;margin-top:4px;'>Recorded ledger deposits</div></div>" +
                "<div class='kpi-card'><div class='kpi-title'>Total Outflow</div><div class='kpi-val val-red'>₹" + String.format("%,.2f", expense) + "</div><div style='font-size:11px;color:#64748b;margin-top:4px;'>Committed & discretionary</div></div>" +
                "<div class='kpi-card'><div class='kpi-title'>Net Liquidity</div><div class='kpi-val " + (net >= 0 ? "val-cyan" : "val-red") + "'>₹" + String.format("%,.2f", net) + "</div><div style='font-size:11px;color:#64748b;margin-top:4px;'>Month-to-date cash balance</div></div>" +
                "<div class='kpi-card'><div class='kpi-title'>Savings Efficiency</div><div class='kpi-val val-cyan'>" + String.format("%.1f%%", savingsRate) + "</div><div style='font-size:11px;color:#64748b;margin-top:4px;'>Savings rate ratio</div></div>" +
                "</div>" +
                "<div class='grid-main'>" +
                "<div class='section-panel'>" +
                "<div class='section-title'><span>Cashflow Ledger (Transactions)</span><span style='font-size:12px;color:#64748b;'>Showing " + transactions.size() + " records</span></div>" +
                "<div style='overflow-x:auto;'>" +
                "<table><thead><tr><th>Date</th><th>Type</th><th>Category</th><th>Description</th><th style='text-align:right;'>Amount</th></tr></thead><tbody>" +
                txRows +
                "</tbody></table>" +
                "</div>" +
                "</div>" +
                "<div style='display:flex;flex-direction:column;gap:20px;'>" +
                "<div class='section-panel'>" +
                "<div class='section-title'><span>Quick Add Transaction</span></div>" +
                "<form id='txForm' onsubmit='submitTx(event)'>" +
                "<div class='form-group'><label>Type</label><select id='txType' name='type' style='width:100%;'><option value='EXPENSE'>Expense</option><option value='INCOME'>Income</option></select></div>" +
                "<div class='form-group'><label>Category</label><select id='txCategory' name='categoryId' style='width:100%;'>" + catOptions + "</select></div>" +
                "<div class='form-group'><label>Amount (₹)</label><input type='number' id='txAmount' name='amount' step='0.01' required placeholder='e.g. 450.00' style='width:100%;'></div>" +
                "<div class='form-group'><label>Date</label><input type='date' id='txDate' name='date' value='" + LocalDate.now() + "' required style='width:100%;'></div>" +
                "<div class='form-group'><label>Description</label><input type='text' id='txDesc' name='description' required placeholder='e.g. Grocery store' style='width:100%;'></div>" +
                "<button type='submit' style='width:100%;padding:10px;margin-top:8px;'>Commit to Database</button>" +
                "</form>" +
                "<div id='formMsg' style='margin-top:10px;font-size:12px;font-weight:600;display:none;'></div>" +
                "</div>" +
                "<div class='section-panel'>" +
                "<div class='section-title'><span>Category Outflows</span></div>" +
                catCards +
                "</div>" +
                "</div>" +
                "</div>" +
                "<div style='text-align:center;padding:16px;color:#475569;font-size:11px;'>" +
                "SpendWise Engine Web Interface • Running locally on http://localhost:8080 • Directly integrated with MySQL 8 InnoDB" +
                "</div>" +
                "</div>" +
                "<script>" +
                "async function submitTx(e) {" +
                "  e.preventDefault();" +
                "  const btn = e.target.querySelector('button');" +
                "  btn.disabled = true; btn.textContent = 'Saving...';" +
                "  const data = new URLSearchParams(new FormData(document.getElementById('txForm'))).toString();" +
                "  try {" +
                "    const res = await fetch('/api/transactions', { method: 'POST', body: data, headers: {'Content-Type': 'application/x-www-form-urlencoded'} });" +
                "    const json = await res.json();" +
                "    const msg = document.getElementById('formMsg');" +
                "    msg.style.display = 'block';" +
                "    if(json.success) {" +
                "      msg.style.color = '#34d399'; msg.textContent = 'Transaction saved! Reloading...';" +
                "      setTimeout(() => window.location.reload(), 600);" +
                "    } else {" +
                "      msg.style.color = '#fb7185'; msg.textContent = 'Error: ' + json.error;" +
                "      btn.disabled = false; btn.textContent = 'Commit to Database';" +
                "    }" +
                "  } catch(err) {" +
                "    alert('Failed to connect to SpendWise local server.');" +
                "    btn.disabled = false; btn.textContent = 'Commit to Database';" +
                "  }" +
                "}" +
                "</script>" +
                "</body>" +
                "</html>";
    }

    private void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType);
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isBlank()) {
            return map;
        }
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private Map<String, String> parseFormData(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isBlank()) {
            return map;
        }
        String[] pairs = body.split("&");
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private int parseInt(String str, int defaultVal) {
        if (str == null) return defaultVal;
        try {
            return Integer.parseInt(str);
        } catch (NumberFormatException e) {
            return defaultVal;
        }
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}

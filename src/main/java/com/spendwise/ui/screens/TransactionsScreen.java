package com.spendwise.ui.screens;

import com.spendwise.service.ValidationException;
import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.model.Transaction;
import com.spendwise.model.TransactionType;
import com.spendwise.service.CategoryService;
import com.spendwise.service.TransactionService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.ConfirmationDialog;
import com.spendwise.ui.components.ScrollablePanel;
import com.spendwise.ui.components.SectionHeader;
import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.components.StyledComboBox;
import com.spendwise.ui.components.StyledTable;
import com.spendwise.ui.components.StyledTextField;
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
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

public class TransactionsScreen extends JPanel implements BaseScreen {

    private final TransactionService transactionService;
    private final CategoryService categoryService;

    private int currentYear;
    private int currentMonth;

    private final DefaultTableModel tableModel;
    private final StyledTable table;
    private final List<Transaction> currentTransactionList = new ArrayList<>();

    private final StyledTextField searchField;
    private final StyledComboBox<String> typeFilter;
    private final StyledComboBox<CategoryItem> categoryFilter;
    private final StyledTextField minAmountField;
    private final StyledTextField maxAmountField;

    public TransactionsScreen(TransactionService transactionService, CategoryService categoryService) {
        this.transactionService = transactionService;
        this.categoryService = categoryService;

        this.currentYear = LocalDate.now().getYear();
        this.currentMonth = LocalDate.now().getMonthValue();

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        ScrollablePanel content = new ScrollablePanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 16, 0));

        SectionHeader header = new SectionHeader("Transactions Ledger", "Search, filter, and record financial movements");
        header.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton addBtn = new StyledButton("+ Record Transaction", StyledButton.ButtonStyle.PRIMARY);
        addBtn.addActionListener(e -> showAddEditDialog(null));
        header.addAction(addBtn);

        content.add(header);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        filterBar.setBackground(AppTheme.CARD_BG);
        filterBar.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterBar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(6, 12, 6, 12)
        ));
        filterBar.setPreferredSize(new Dimension(800, 54));
        filterBar.setMaximumSize(new Dimension(3000, 54));

        searchField = new StyledTextField("Search description...", 14);
        typeFilter = new StyledComboBox<>(new String[]{"All Types", "Income", "Expense"});
        categoryFilter = new StyledComboBox<>();
        categoryFilter.setPreferredSize(new Dimension(160, 36));

        minAmountField = new StyledTextField("Min Rs", 6);
        maxAmountField = new StyledTextField("Max Rs", 6);

        StyledButton filterBtn = new StyledButton("Filter", StyledButton.ButtonStyle.SECONDARY);
        filterBtn.addActionListener(e -> applyFilters());

        StyledButton clearBtn = new StyledButton("Clear", StyledButton.ButtonStyle.GHOST);
        clearBtn.addActionListener(e -> clearFilters());

        filterBar.add(searchField);
        filterBar.add(typeFilter);
        filterBar.add(categoryFilter);
        filterBar.add(minAmountField);
        filterBar.add(maxAmountField);
        filterBar.add(filterBtn);
        filterBar.add(clearBtn);

        content.add(filterBar);
        content.add(Box.createVerticalStrut(16));

        String[] cols = {"ID", "Date", "Category", "Description", "Type", "Amount"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new StyledTable(tableModel);
        table.setAutoCreateRowSorter(true);
        table.getColumnModel().getColumn(0).setMaxWidth(60);

        JScrollPane tableScroll = new JScrollPane(table);
        tableScroll.setAlignmentX(Component.LEFT_ALIGNMENT);
        tableScroll.setBackground(AppTheme.CARD_BG);
        tableScroll.getViewport().setBackground(AppTheme.CARD_BG);
        tableScroll.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        tableScroll.setPreferredSize(new Dimension(800, 420));
        tableScroll.setMaximumSize(new Dimension(3000, 600));
        content.add(tableScroll);

        content.add(Box.createVerticalStrut(12));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);
        actionRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton editBtn = new StyledButton("Edit Selected", StyledButton.ButtonStyle.SECONDARY);
        editBtn.addActionListener(e -> onEditSelected());

        StyledButton deleteBtn = new StyledButton("Delete Selected", StyledButton.ButtonStyle.DANGER);
        deleteBtn.addActionListener(e -> onDeleteSelected());

        actionRow.add(editBtn);
        actionRow.add(deleteBtn);
        content.add(actionRow);

        add(content, BorderLayout.CENTER);
    }

    @Override
    public void addNotify() {
        super.addNotify();
        reloadCategoryFilter();
        loadTransactionsForCurrentMonth();
    }

    @Override
    public void onScreenSelected() {
        reloadCategoryFilter();
        loadTransactionsForCurrentMonth();
    }

    @Override
    public void onMonthChanged(int year, int month) {
        this.currentYear = year;
        this.currentMonth = month;
        loadTransactionsForCurrentMonth();
    }

    private void reloadCategoryFilter() {
        AsyncWorker.execute(
                () -> categoryService.getCategories(1L),
                cats -> {
                    categoryFilter.removeAllItems();
                    categoryFilter.addItem(new CategoryItem(null, "All Categories"));
                    for (Category c : cats) {
                        categoryFilter.addItem(new CategoryItem(c.getCategoryId(), c.getCategoryName()));
                    }
                },
                null
        );
    }

    private void loadTransactionsForCurrentMonth() {
        YearMonth ym = YearMonth.of(currentYear, currentMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        AsyncWorker.execute(
                () -> transactionService.getFilteredTransactions(1L, start, end, null, null, null, null, null),
                this::populateTable,
                err -> showToast("Error loading transactions: " + err.getMessage(), Toast.ToastType.ERROR)
        );
    }

    private void applyFilters() {
        String search = searchField.getText().trim();
        if (search.isEmpty()) search = null;

        String typeStr = (String) typeFilter.getSelectedItem();
        TransactionType type = null;
        if ("Income".equalsIgnoreCase(typeStr)) type = TransactionType.INCOME;
        else if ("Expense".equalsIgnoreCase(typeStr)) type = TransactionType.EXPENSE;

        CategoryItem catItem = (CategoryItem) categoryFilter.getSelectedItem();
        Long catId = catItem != null ? catItem.categoryId : null;

        BigDecimal min = null;
        if (!minAmountField.getText().trim().isEmpty()) {
            try {
                min = MoneyUtil.parse(minAmountField.getText());
            } catch (Exception ignored) {
            }
        }

        BigDecimal max = null;
        if (!maxAmountField.getText().trim().isEmpty()) {
            try {
                max = MoneyUtil.parse(maxAmountField.getText());
            } catch (Exception ignored) {
            }
        }

        YearMonth ym = YearMonth.of(currentYear, currentMonth);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();

        final BigDecimal finalMin = min;
        final BigDecimal finalMax = max;
        final TransactionType finalType = type;
        final String finalSearch = search;

        AsyncWorker.execute(
                () -> transactionService.getFilteredTransactions(1L, start, end, catId, finalType, finalMin, finalMax, finalSearch),
                this::populateTable,
                err -> showToast("Filter failed: " + err.getMessage(), Toast.ToastType.ERROR)
        );
    }

    private void clearFilters() {
        searchField.setText("");
        typeFilter.setSelectedIndex(0);
        categoryFilter.setSelectedIndex(0);
        minAmountField.setText("");
        maxAmountField.setText("");
        loadTransactionsForCurrentMonth();
    }

    private void populateTable(List<Transaction> transactions) {
        currentTransactionList.clear();
        currentTransactionList.addAll(transactions);
        tableModel.setRowCount(0);

        for (Transaction t : transactions) {
            String typeIndicator = t.getTransactionType() == TransactionType.INCOME ? "[+] Income" : "[-] Expense";
            tableModel.addRow(new Object[]{
                    t.getTransactionId(),
                    t.getTransactionDate().toString(),
                    t.getCategoryName(),
                    t.getDescription(),
                    typeIndicator,
                    MoneyUtil.formatRupees(t.getAmount())
            });
        }
    }

    private void onEditSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showToast("Please select a transaction to edit.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);

        Transaction target = currentTransactionList.stream()
                .filter(t -> t.getTransactionId().equals(id))
                .findFirst()
                .orElse(null);

        if (target != null) {
            showAddEditDialog(target);
        }
    }

    private void onDeleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            showToast("Please select a transaction to delete.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = table.convertRowIndexToModel(row);
        Long id = (Long) tableModel.getValueAt(modelRow, 0);
        String desc = (String) tableModel.getValueAt(modelRow, 3);
        String amount = (String) tableModel.getValueAt(modelRow, 5);

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.show(
                topFrame,
                "Delete Transaction",
                "Are you sure you want to delete transaction #" + id + " (" + desc + " — " + amount + ")? This action cannot be undone.",
                "Delete",
                true
        );

        if (confirmed) {
            AsyncWorker.execute(
                    () -> transactionService.deleteTransaction(id),
                    success -> {
                        showToast("Transaction deleted successfully.", Toast.ToastType.SUCCESS);
                        loadTransactionsForCurrentMonth();
                    },
                    err -> showToast("Delete failed: " + err.getMessage(), Toast.ToastType.ERROR)
            );
        }
    }

    private void showAddEditDialog(Transaction existing) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(topFrame, existing == null ? "Record Transaction" : "Edit Transaction #" + existing.getTransactionId(), true);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);

        JPanel form = new JPanel(new GridLayout(6, 2, 12, 14));
        form.setBackground(AppTheme.CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));

        StyledComboBox<String> typeCombo = new StyledComboBox<>(new String[]{"EXPENSE", "INCOME"});
        StyledComboBox<CategoryItem> catCombo = new StyledComboBox<>();
        StyledTextField amountInput = new StyledTextField(10);
        StyledTextField dateInput = new StyledTextField(10);
        dateInput.setText(LocalDate.now().toString());
        StyledTextField descInput = new StyledTextField(15);
        JLabel validationMsg = new JLabel("");
        validationMsg.setFont(AppTheme.FONT_SMALL);
        validationMsg.setForeground(AppTheme.DANGER);

        Runnable refreshCatList = () -> {
            String selectedType = (String) typeCombo.getSelectedItem();
            CategoryType ct = "INCOME".equalsIgnoreCase(selectedType) ? CategoryType.INCOME : CategoryType.EXPENSE;
            AsyncWorker.execute(
                    () -> categoryService.getCategoriesByType(1L, ct),
                    cats -> {
                        catCombo.removeAllItems();
                        for (Category c : cats) {
                            catCombo.addItem(new CategoryItem(c.getCategoryId(), c.getCategoryName()));
                        }
                        if (existing != null) {
                            for (int i = 0; i < catCombo.getItemCount(); i++) {
                                if (catCombo.getItemAt(i).categoryId.equals(existing.getCategoryId())) {
                                    catCombo.setSelectedIndex(i);
                                    break;
                                }
                            }
                        }
                    },
                    null
            );
        };

        typeCombo.addActionListener(e -> refreshCatList.run());

        if (existing != null) {
            typeCombo.setSelectedItem(existing.getTransactionType().name());
            amountInput.setText(existing.getAmount().toPlainString());
            dateInput.setText(existing.getTransactionDate().toString());
            descInput.setText(existing.getDescription());
        }

        refreshCatList.run();

        form.add(new JLabel("Transaction Type:"));
        form.add(typeCombo);
        form.add(new JLabel("Category:"));
        form.add(catCombo);
        form.add(new JLabel("Amount (Rs):"));
        form.add(amountInput);
        form.add(new JLabel("Date (YYYY-MM-DD):"));
        form.add(dateInput);
        form.add(new JLabel("Description:"));
        form.add(descInput);
        form.add(new JLabel(""));
        form.add(validationMsg);

        dlg.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(AppTheme.CARD_BG);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.CARD_BORDER));

        StyledButton cancelBtn = new StyledButton("Cancel", StyledButton.ButtonStyle.SECONDARY);
        cancelBtn.addActionListener(e -> dlg.dispose());

        StyledButton saveBtn = new StyledButton(existing == null ? "Save Transaction" : "Update Transaction", StyledButton.ButtonStyle.PRIMARY);
        saveBtn.addActionListener(e -> {
            try {
                String typeStr = (String) typeCombo.getSelectedItem();
                TransactionType type = TransactionType.valueOf(typeStr);

                CategoryItem catItem = (CategoryItem) catCombo.getSelectedItem();
                if (catItem == null || catItem.categoryId == null) {
                    validationMsg.setText("Please select a category.");
                    return;
                }

                String amtStr = amountInput.getText().trim();
                if (amtStr.isEmpty()) {
                    validationMsg.setText("Please enter an amount.");
                    return;
                }
                BigDecimal amt;
                try {
                    amt = MoneyUtil.parse(amtStr);
                    if (amt.compareTo(BigDecimal.ZERO) <= 0) {
                        validationMsg.setText("Amount must be greater than zero.");
                        return;
                    }
                } catch (Exception ex) {
                    validationMsg.setText("Please enter a valid numeric amount.");
                    return;
                }

                LocalDate d;
                try {
                    d = parseDate(dateInput.getText());
                } catch (ValidationException ex) {
                    validationMsg.setText(ex.getMessage());
                    return;
                }

                String desc = descInput.getText().trim();
                if (desc.isEmpty()) {
                    validationMsg.setText("Please enter a description.");
                    return;
                }

                if (existing == null) {
                    AsyncWorker.execute(
                            () -> transactionService.addTransaction(1L, catItem.categoryId, type, amt, d, desc),
                            saved -> {
                                dlg.dispose();
                                showToast("Transaction saved successfully.", Toast.ToastType.SUCCESS);
                                loadTransactionsForCurrentMonth();
                            },
                            err -> validationMsg.setText(err.getMessage())
                    );
                } else {
                    AsyncWorker.execute(
                            () -> transactionService.updateTransaction(1L, existing.getTransactionId(), catItem.categoryId, type, amt, d, desc),
                            updated -> {
                                dlg.dispose();
                                showToast("Transaction updated successfully.", Toast.ToastType.SUCCESS);
                                loadTransactionsForCurrentMonth();
                            },
                            err -> validationMsg.setText(err.getMessage())
                    );
                }
            } catch (Exception ex) {
                validationMsg.setText(ex.getMessage());
            }
        });

        buttonPanel.add(cancelBtn);
        buttonPanel.add(saveBtn);
        dlg.add(buttonPanel, BorderLayout.SOUTH);

        dlg.setPreferredSize(new Dimension(480, 410));
        dlg.pack();
        dlg.setLocationRelativeTo(topFrame);
        dlg.setVisible(true);
    }

    private LocalDate parseDate(String input) {
        if (input == null || input.trim().isEmpty()) {
            throw new ValidationException("Please enter a transaction date.");
        }
        String s = input.trim();
        List<DateTimeFormatter> formatters = List.of(
                DateTimeFormatter.ISO_LOCAL_DATE,
                DateTimeFormatter.ofPattern("dd-MM-yyyy"),
                DateTimeFormatter.ofPattern("dd/MM/yyyy"),
                DateTimeFormatter.ofPattern("yyyy/MM/dd"),
                DateTimeFormatter.ofPattern("dd.MM.yyyy")
        );
        for (DateTimeFormatter dtf : formatters) {
            try {
                return LocalDate.parse(s, dtf);
            } catch (DateTimeParseException ignored) {
            }
        }
        throw new ValidationException("Invalid date format. Please use YYYY-MM-DD or DD-MM-YYYY.");
    }

    private void showToast(String message, Toast.ToastType type) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Toast.show(topFrame, message, type);
    }

    private record CategoryItem(Long categoryId, String categoryName) {
        @Override
        public String toString() {
            return categoryName;
        }
    }
}

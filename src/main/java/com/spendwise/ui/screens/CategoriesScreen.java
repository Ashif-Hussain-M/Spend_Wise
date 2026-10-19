package com.spendwise.ui.screens;

import com.spendwise.model.Category;
import com.spendwise.model.CategoryType;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ValidationException;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.ConfirmationDialog;
import com.spendwise.ui.components.SectionHeader;
import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.components.StyledComboBox;
import com.spendwise.ui.components.StyledTable;
import com.spendwise.ui.components.StyledTextField;
import com.spendwise.ui.components.Toast;
import com.spendwise.ui.theme.AppIcons;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.List;

public class CategoriesScreen extends JPanel implements BaseScreen {

    private final CategoryService categoryService;

    private final DefaultTableModel expenseModel;
    private final StyledTable expenseTable;
    private final List<Category> expenseList = new ArrayList<>();

    private final DefaultTableModel incomeModel;
    private final StyledTable incomeTable;
    private final List<Category> incomeList = new ArrayList<>();

    private final JTabbedPane tabbedPane;

    public CategoriesScreen(CategoryService categoryService) {
        this.categoryService = categoryService;

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 16, 0));

        SectionHeader header = new SectionHeader("Category Management", "Organize transactions under Income and Expense categories");

        StyledButton addBtn = new StyledButton("+ New Category", StyledButton.ButtonStyle.PRIMARY);
        addBtn.addActionListener(e -> showAddEditDialog(null, getActiveTabType()));
        header.addAction(addBtn);

        content.add(header);

        String[] cols = {"ID", "Category Name", "Recorded Transactions", "Created At"};

        expenseModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        expenseTable = new StyledTable(expenseModel);
        expenseTable.getColumnModel().getColumn(0).setMaxWidth(60);

        incomeModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        incomeTable = new StyledTable(incomeModel);
        incomeTable.getColumnModel().getColumn(0).setMaxWidth(60);

        tabbedPane = new JTabbedPane();
        tabbedPane.setFont(AppTheme.FONT_BODY_BOLD);
        tabbedPane.setBackground(AppTheme.CARD_BG);
        tabbedPane.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel expenseTab = createTablePanel(expenseTable);
        JPanel incomeTab = createTablePanel(incomeTable);

        tabbedPane.addTab("Expense Categories", expenseTab);
        tabbedPane.addTab("Income Categories", incomeTab);

        content.add(tabbedPane);
        content.add(Box.createVerticalStrut(14));

        JPanel actionRow = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        actionRow.setOpaque(false);

        StyledButton editBtn = new StyledButton("Edit Category", StyledButton.ButtonStyle.SECONDARY);
        editBtn.addActionListener(e -> onEditSelected());

        StyledButton deleteBtn = new StyledButton("Delete Category", StyledButton.ButtonStyle.DANGER);
        deleteBtn.addActionListener(e -> onDeleteSelected());

        actionRow.add(editBtn);
        actionRow.add(deleteBtn);
        content.add(actionRow);

        add(content, BorderLayout.CENTER);
    }

    private JPanel createTablePanel(StyledTable table) {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(AppTheme.CARD_BG);
        panel.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBackground(AppTheme.CARD_BG);
        scroll.getViewport().setBackground(AppTheme.CARD_BG);
        scroll.setBorder(null);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    @Override
    public void onScreenSelected() {
        loadCategories();
    }

    @Override
    public void onMonthChanged(int year, int month) {
        loadCategories();
    }

    public void loadCategories() {
        AsyncWorker.execute(
                () -> {
                    List<Category> expenses = categoryService.getCategoriesByType(1L, CategoryType.EXPENSE);
                    List<Category> incomes = categoryService.getCategoriesByType(1L, CategoryType.INCOME);
                    return new CategoryPayload(expenses, incomes);
                },
                this::applyCategories,
                err -> showToast("Error loading categories: " + err.getMessage(), Toast.ToastType.ERROR)
        );
    }

    private void applyCategories(CategoryPayload payload) {
        expenseList.clear();
        expenseList.addAll(payload.expenses);
        expenseModel.setRowCount(0);
        for (Category c : payload.expenses) {
            expenseModel.addRow(new Object[]{
                    c.getCategoryId(),
                    c.getCategoryName(),
                    c.getTransactionCount() + " transaction(s)",
                    c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "Initial"
            });
        }

        incomeList.clear();
        incomeList.addAll(payload.incomes);
        incomeModel.setRowCount(0);
        for (Category c : payload.incomes) {
            incomeModel.addRow(new Object[]{
                    c.getCategoryId(),
                    c.getCategoryName(),
                    c.getTransactionCount() + " transaction(s)",
                    c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate().toString() : "Initial"
            });
        }
    }

    private CategoryType getActiveTabType() {
        return tabbedPane.getSelectedIndex() == 0 ? CategoryType.EXPENSE : CategoryType.INCOME;
    }

    private StyledTable getActiveTable() {
        return tabbedPane.getSelectedIndex() == 0 ? expenseTable : incomeTable;
    }

    private DefaultTableModel getActiveModel() {
        return tabbedPane.getSelectedIndex() == 0 ? expenseModel : incomeModel;
    }

    private List<Category> getActiveList() {
        return tabbedPane.getSelectedIndex() == 0 ? expenseList : incomeList;
    }

    private void onEditSelected() {
        StyledTable activeTable = getActiveTable();
        int row = activeTable.getSelectedRow();
        if (row < 0) {
            showToast("Please select a category to edit.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = activeTable.convertRowIndexToModel(row);
        Long id = (Long) getActiveModel().getValueAt(modelRow, 0);

        Category target = getActiveList().stream()
                .filter(c -> c.getCategoryId().equals(id))
                .findFirst()
                .orElse(null);

        if (target != null) {
            showAddEditDialog(target, target.getCategoryType());
        }
    }

    private void onDeleteSelected() {
        StyledTable activeTable = getActiveTable();
        int row = activeTable.getSelectedRow();
        if (row < 0) {
            showToast("Please select a category to delete.", Toast.ToastType.INFO);
            return;
        }
        int modelRow = activeTable.convertRowIndexToModel(row);
        Long id = (Long) getActiveModel().getValueAt(modelRow, 0);
        String name = (String) getActiveModel().getValueAt(modelRow, 1);

        Category target = getActiveList().stream()
                .filter(c -> c.getCategoryId().equals(id))
                .findFirst()
                .orElse(null);

        if (target != null && target.getTransactionCount() > 0) {
            JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
            ConfirmationDialog.show(
                    topFrame,
                    "Deletion Blocked",
                    "Category '" + name + "' is in use by " + target.getTransactionCount() +
                            " transaction(s) and cannot be deleted. Delete policy: ON DELETE RESTRICT.",
                    "Close",
                    false
            );
            return;
        }

        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        boolean confirmed = ConfirmationDialog.show(
                topFrame,
                "Delete Category",
                "Are you sure you want to delete category '" + name + "'?",
                "Delete",
                true
        );

        if (confirmed) {
            AsyncWorker.execute(
                    () -> categoryService.deleteCategory(id),
                    success -> {
                        showToast("Category deleted successfully.", Toast.ToastType.SUCCESS);
                        loadCategories();
                    },
                    err -> showToast("Delete failed: " + err.getMessage(), Toast.ToastType.ERROR)
            );
        }
    }

    private void showAddEditDialog(Category existing, CategoryType defaultType) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(topFrame, existing == null ? "Create Category" : "Edit Category #" + existing.getCategoryId(), true);
        dlg.setLayout(new BorderLayout());
        dlg.setResizable(false);

        JPanel form = new JPanel(new GridLayout(3, 2, 12, 14));
        form.setBackground(AppTheme.CARD_BG);
        form.setBorder(new EmptyBorder(20, 24, 20, 24));

        StyledTextField nameInput = new StyledTextField(15);
        StyledComboBox<String> typeCombo = new StyledComboBox<>(new String[]{"EXPENSE", "INCOME"});
        JLabel validationMsg = new JLabel("");
        validationMsg.setFont(AppTheme.FONT_SMALL);
        validationMsg.setForeground(AppTheme.DANGER);

        if (existing != null) {
            nameInput.setText(existing.getCategoryName());
            typeCombo.setSelectedItem(existing.getCategoryType().name());
            if (existing.getTransactionCount() > 0) {
                typeCombo.setEnabled(false);
            }
        } else if (defaultType != null) {
            typeCombo.setSelectedItem(defaultType.name());
        }

        form.add(new JLabel("Category Name:"));
        form.add(nameInput);
        form.add(new JLabel("Category Type:"));
        form.add(typeCombo);
        form.add(new JLabel(""));
        form.add(validationMsg);

        dlg.add(form, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBackground(AppTheme.CARD_BG);
        buttonPanel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.CARD_BORDER));

        StyledButton cancelBtn = new StyledButton("Cancel", StyledButton.ButtonStyle.SECONDARY);
        cancelBtn.addActionListener(e -> dlg.dispose());

        StyledButton saveBtn = new StyledButton(existing == null ? "Create" : "Save", StyledButton.ButtonStyle.PRIMARY);
        saveBtn.addActionListener(e -> {
            String name = nameInput.getText().trim();
            CategoryType type = CategoryType.valueOf((String) typeCombo.getSelectedItem());

            if (name.isEmpty()) {
                validationMsg.setText("Category name cannot be empty.");
                return;
            }

            if (existing == null) {
                AsyncWorker.execute(
                        () -> categoryService.createCategory(1L, name, type),
                        saved -> {
                            dlg.dispose();
                            showToast("Category created successfully.", Toast.ToastType.SUCCESS);
                            loadCategories();
                        },
                        err -> validationMsg.setText(err.getMessage())
                );
            } else {
                AsyncWorker.execute(
                        () -> categoryService.updateCategory(1L, existing.getCategoryId(), name, type),
                        updated -> {
                            dlg.dispose();
                            showToast("Category updated successfully.", Toast.ToastType.SUCCESS);
                            loadCategories();
                        },
                        err -> validationMsg.setText(err.getMessage())
                );
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

    private record CategoryPayload(List<Category> expenses, List<Category> incomes) {
    }
}

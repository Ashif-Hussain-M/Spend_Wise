package com.spendwise.ui.screens;

import com.spendwise.service.CategoryService;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class CategoriesScreen extends JPanel implements BaseScreen {

    private final CategoryService categoryService;

    public CategoriesScreen(CategoryService categoryService) {
        this.categoryService = categoryService;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);
        add(new JLabel("Categories Screen", JLabel.CENTER), BorderLayout.CENTER);
    }

    @Override
    public void onScreenSelected() {
    }

    @Override
    public void onMonthChanged(int year, int month) {
    }
}

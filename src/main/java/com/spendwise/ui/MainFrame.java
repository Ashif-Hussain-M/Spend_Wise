package com.spendwise.ui;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.service.BudgetService;
import com.spendwise.service.CategoryService;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.service.TransactionService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.components.StyledComboBox;
import com.spendwise.ui.screens.BaseScreen;
import com.spendwise.ui.screens.WelcomeScreen;
import com.spendwise.ui.theme.AppIcons;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainFrame extends JFrame {

    private final CategoryService categoryService;
    private final TransactionService transactionService;
    private final BudgetService budgetService;
    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;

    private final CardLayout cardLayout;
    private final JPanel contentPanel;
    private final JLabel headerTitleLabel;
    private final StyledComboBox<String> monthCombo;
    private final StyledComboBox<Integer> yearCombo;
    private final JLabel dbStatusLabel;

    private final List<NavButton> navButtons = new ArrayList<>();
    private final Map<String, BaseScreen> screenMap = new HashMap<>();
    private String currentScreenKey = "DASHBOARD";

    private int selectedYear = LocalDate.now().getYear();
    private int selectedMonth = LocalDate.now().getMonthValue();

    public MainFrame(CategoryService categoryService,
                     TransactionService transactionService,
                     BudgetService budgetService,
                     ReportService reportService,
                     SpendingAnalysisService analysisService) {
        super("SpendWise — Your money. Your clarity.");
        this.categoryService = categoryService;
        this.transactionService = transactionService;
        this.budgetService = budgetService;
        this.reportService = reportService;
        this.analysisService = analysisService;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1100, 720));
        setPreferredSize(new Dimension(1200, 800));
        setLocationRelativeTo(null);

        CardLayout rootCardLayout = new CardLayout();
        JPanel rootContainer = new JPanel(rootCardLayout);
        rootContainer.setBackground(AppTheme.BG_DARK);

        JPanel appShell = new JPanel(new BorderLayout());
        appShell.setBackground(AppTheme.BG_DARK);

        JPanel sidebar = buildSidebar();
        appShell.add(sidebar, BorderLayout.WEST);

        JPanel mainArea = new JPanel(new BorderLayout());
        mainArea.setBackground(AppTheme.BG_DARK);

        headerTitleLabel = new JLabel("Dashboard");
        headerTitleLabel.setFont(AppTheme.FONT_TITLE);
        headerTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);

        String[] monthNames = new String[12];
        for (int i = 1; i <= 12; i++) {
            monthNames[i - 1] = Month.of(i).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
        }
        monthCombo = new StyledComboBox<>(monthNames);
        monthCombo.setSelectedIndex(selectedMonth - 1);

        Integer[] years = {2024, 2025, 2026, 2027, 2028};
        yearCombo = new StyledComboBox<>(years);
        yearCombo.setSelectedItem(selectedYear);

        dbStatusLabel = new JLabel("Checking DB...");
        dbStatusLabel.setFont(AppTheme.FONT_SMALL);
        dbStatusLabel.setForeground(AppTheme.TEXT_MUTED);

        JPanel header = buildHeader();
        mainArea.add(header, BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(AppTheme.BG_DARK);
        contentPanel.setBorder(new EmptyBorder(12, 24, 20, 24));

        mainArea.add(contentPanel, BorderLayout.CENTER);
        appShell.add(mainArea, BorderLayout.CENTER);

        WelcomeScreen welcomeScreen = new WelcomeScreen(() -> {
            rootCardLayout.show(rootContainer, "APP_SHELL");
            showScreen("DASHBOARD");
        });

        rootContainer.add(welcomeScreen, "WELCOME");
        rootContainer.add(appShell, "APP_SHELL");

        setContentPane(rootContainer);
        rootCardLayout.show(rootContainer, "WELCOME");

        monthCombo.addActionListener(e -> onDateChanged());
        yearCombo.addActionListener(e -> onDateChanged());

        checkDatabaseStatus();
    }

    public void registerScreen(String key, String title, BaseScreen screen) {
        screenMap.put(key, screen);
        contentPanel.add((JPanel) screen, key);
    }

    public void showScreen(String key) {
        currentScreenKey = key;
        cardLayout.show(contentPanel, key);

        for (NavButton btn : navButtons) {
            btn.setActive(btn.screenKey.equals(key));
        }

        BaseScreen screen = screenMap.get(key);
        if (screen != null) {
            headerTitleLabel.setText(getScreenTitle(key));
            screen.onScreenSelected();
            screen.onMonthChanged(selectedYear, selectedMonth);
        }
    }

    public void refreshCurrentScreen() {
        BaseScreen screen = screenMap.get(currentScreenKey);
        if (screen != null) {
            screen.onMonthChanged(selectedYear, selectedMonth);
        }
        checkDatabaseStatus();
    }

    public int getSelectedYear() {
        return selectedYear;
    }

    public int getSelectedMonth() {
        return selectedMonth;
    }

    private void onDateChanged() {
        selectedMonth = monthCombo.getSelectedIndex() + 1;
        Integer yr = (Integer) yearCombo.getSelectedItem();
        if (yr != null) {
            selectedYear = yr;
        }
        BaseScreen screen = screenMap.get(currentScreenKey);
        if (screen != null) {
            screen.onMonthChanged(selectedYear, selectedMonth);
        }
    }

    private void checkDatabaseStatus() {
        AsyncWorker.execute(
                DatabaseConnection::checkConnectivity,
                connected -> {
                    if (connected) {
                        dbStatusLabel.setText("● Connected");
                        dbStatusLabel.setForeground(AppTheme.SUCCESS);
                    } else {
                        dbStatusLabel.setText("● Offline");
                        dbStatusLabel.setForeground(AppTheme.DANGER);
                    }
                },
                err -> {
                    dbStatusLabel.setText("● Offline");
                    dbStatusLabel.setForeground(AppTheme.DANGER);
                }
        );
    }

    private JPanel buildSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setLayout(new BoxLayout(sidebar, BoxLayout.Y_AXIS));
        sidebar.setBackground(AppTheme.SIDEBAR_BG);
        sidebar.setPreferredSize(new Dimension(240, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.CARD_BORDER));

        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 24, 20));

        JLabel brandTitle = new JLabel("SpendWise");
        brandTitle.setFont(AppTheme.FONT_TITLE);
        brandTitle.setForeground(AppTheme.PRIMARY_ACCENT);

        JLabel brandTagline = new JLabel("Your money. Your clarity.");
        brandTagline.setFont(AppTheme.FONT_SMALL);
        brandTagline.setForeground(AppTheme.TEXT_SECONDARY);

        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(brandTagline);

        sidebar.add(brandPanel);

        addNavButton(sidebar, "DASHBOARD", "Dashboard", AppIcons.dashboard(18, null));
        addNavButton(sidebar, "TRANSACTIONS", "Transactions", AppIcons.transactions(18, null));
        addNavButton(sidebar, "CATEGORIES", "Categories", AppIcons.categories(18, null));
        addNavButton(sidebar, "BUDGETS", "Budgets", AppIcons.budgets(18, null));
        addNavButton(sidebar, "REPORTS", "Reports", AppIcons.reports(18, null));
        addNavButton(sidebar, "SETTINGS", "Settings", AppIcons.settings(18, null));

        sidebar.add(Box.createVerticalGlue());

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        footerPanel.setOpaque(false);
        JLabel versionLabel = new JLabel("v1.0.0 • Java 21");
        versionLabel.setFont(AppTheme.FONT_SMALL);
        versionLabel.setForeground(AppTheme.TEXT_MUTED);
        footerPanel.add(versionLabel);
        sidebar.add(footerPanel);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String screenKey, String label, Icon icon) {
        NavButton btn = new NavButton(screenKey, label, icon);
        navButtons.add(btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(4));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        header.setBorder(new EmptyBorder(18, 24, 12, 24));

        header.add(headerTitleLabel, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);

        controls.add(dbStatusLabel);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(monthCombo);
        controls.add(yearCombo);

        JButton refreshBtn = new JButton(AppIcons.refresh(16, AppTheme.TEXT_PRIMARY));
        refreshBtn.setPreferredSize(new Dimension(36, 36));
        refreshBtn.setBackground(AppTheme.CARD_BG);
        refreshBtn.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setToolTipText("Refresh data");
        refreshBtn.addActionListener(e -> refreshCurrentScreen());
        controls.add(refreshBtn);

        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private String getScreenTitle(String key) {
        return switch (key) {
            case "DASHBOARD" -> "Dashboard";
            case "TRANSACTIONS" -> "Transactions";
            case "CATEGORIES" -> "Category Management";
            case "BUDGETS" -> "Budgets & Spending Limits";
            case "REPORTS" -> "Financial Reports & Insights";
            case "SETTINGS" -> "Preferences & Diagnostics";
            default -> "SpendWise";
        };
    }

    private class NavButton extends JPanel {
        private final String screenKey;
        private final JLabel label;
        private boolean active = false;
        private boolean hovered = false;

        public NavButton(String screenKey, String text, Icon icon) {
            this.screenKey = screenKey;
            setLayout(new BorderLayout(12, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(240, 42));
            setPreferredSize(new Dimension(240, 42));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 20, 8, 20));

            label = new JLabel(text, icon, JLabel.LEFT);
            label.setIconTextGap(12);
            label.setFont(AppTheme.FONT_BODY_BOLD);
            label.setForeground(AppTheme.TEXT_SECONDARY);

            add(label, BorderLayout.CENTER);

            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    hovered = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    hovered = false;
                    repaint();
                }

                @Override
                public void mouseClicked(MouseEvent e) {
                    showScreen(screenKey);
                }
            });
        }

        public void setActive(boolean active) {
            this.active = active;
            label.setForeground(active ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (active) {
                g2.setColor(AppTheme.CARD_BG);
                g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
                g2.setColor(AppTheme.PRIMARY_ACCENT);
                g2.fillRoundRect(8, 8, 4, getHeight() - 16, 2, 2);
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 10));
                g2.fillRoundRect(8, 2, getWidth() - 16, getHeight() - 4, 8, 8);
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

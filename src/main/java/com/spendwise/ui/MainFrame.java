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
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URI;
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
        sidebar.setPreferredSize(new Dimension(250, 0));
        sidebar.setBorder(BorderFactory.createMatteBorder(0, 0, 0, 1, AppTheme.CARD_BORDER));

        JPanel brandPanel = new JPanel();
        brandPanel.setLayout(new BoxLayout(brandPanel, BoxLayout.Y_AXIS));
        brandPanel.setOpaque(false);
        brandPanel.setBorder(new EmptyBorder(24, 20, 20, 20));

        JLabel brandTitle = new JLabel("SPENDWISE // ENGINE");
        brandTitle.setFont(AppTheme.FONT_HEADER);
        brandTitle.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel brandTagline = new JLabel("Autonomous cashflow engine");
        brandTagline.setFont(AppTheme.FONT_SMALL);
        brandTagline.setForeground(AppTheme.TEXT_MUTED);

        JPanel statusPill = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        statusPill.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.BOLD, 9));
        dot.setForeground(AppTheme.PRIMARY_ACCENT);
        JLabel statusText = new JLabel("DEMO SESSION ACTIVE");
        statusText.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        statusText.setForeground(AppTheme.PRIMARY_ACCENT);
        statusPill.add(dot);
        statusPill.add(statusText);

        brandPanel.add(brandTitle);
        brandPanel.add(Box.createVerticalStrut(4));
        brandPanel.add(brandTagline);
        brandPanel.add(Box.createVerticalStrut(8));
        brandPanel.add(statusPill);

        sidebar.add(brandPanel);
        sidebar.add(Box.createVerticalStrut(8));

        addNavButton(sidebar, "DASHBOARD", "Decision Center", "1 P1", AppIcons.dashboard(16, null));
        addNavButton(sidebar, "TRANSACTIONS", "Cashflow Ledger", null, AppIcons.transactions(16, null));
        addNavButton(sidebar, "CATEGORIES", "Category Control", null, AppIcons.categories(16, null));
        addNavButton(sidebar, "BUDGETS", "Guardrails & Limits", null, AppIcons.budgets(16, null));
        addNavButton(sidebar, "REPORTS", "Profit & What-If", null, AppIcons.reports(16, null));
        addNavButton(sidebar, "SETTINGS", "Engine Preferences", null, AppIcons.settings(16, null));

        sidebar.add(Box.createVerticalGlue());

        JPanel footerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 16));
        footerPanel.setOpaque(false);
        JLabel versionLabel = new JLabel("v1.0.0 • Java 21 + MySQL");
        versionLabel.setFont(AppTheme.FONT_SMALL);
        versionLabel.setForeground(AppTheme.TEXT_MUTED);
        footerPanel.add(versionLabel);
        sidebar.add(footerPanel);

        return sidebar;
    }

    private void addNavButton(JPanel sidebar, String screenKey, String label, String tag, Icon icon) {
        NavButton btn = new NavButton(screenKey, label, tag, icon);
        navButtons.add(btn);
        sidebar.add(btn);
        sidebar.add(Box.createVerticalStrut(2));
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(AppTheme.SIDEBAR_BG);
        header.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.CARD_BORDER),
                new EmptyBorder(14, 24, 14, 24)
        ));

        JPanel breadcrumb = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        breadcrumb.setOpaque(false);
        JLabel prefix = new JLabel("SPENDWISE /");
        prefix.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        prefix.setForeground(AppTheme.TEXT_MUTED);
        headerTitleLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 12));
        headerTitleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        breadcrumb.add(prefix);
        breadcrumb.add(headerTitleLabel);

        header.add(breadcrumb, BorderLayout.WEST);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        controls.setOpaque(false);

        dbStatusLabel.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        controls.add(dbStatusLabel);
        controls.add(Box.createHorizontalStrut(8));
        controls.add(monthCombo);
        controls.add(yearCombo);

        JButton refreshBtn = new JButton(AppIcons.refresh(14, AppTheme.TEXT_PRIMARY));
        refreshBtn.setPreferredSize(new Dimension(34, 34));
        refreshBtn.setBackground(AppTheme.CARD_BG);
        refreshBtn.setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        refreshBtn.setFocusPainted(false);
        refreshBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        refreshBtn.setToolTipText("Refresh data");
        refreshBtn.addActionListener(e -> refreshCurrentScreen());
        controls.add(refreshBtn);

        JButton webBtn = new JButton("Web: 8080");
        webBtn.setPreferredSize(new Dimension(85, 34));
        webBtn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        webBtn.setBackground(new Color(16, 185, 129, 25));
        webBtn.setForeground(AppTheme.PRIMARY_ACCENT);
        webBtn.setBorder(BorderFactory.createLineBorder(new Color(16, 185, 129, 80), 1));
        webBtn.setFocusPainted(false);
        webBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        webBtn.setToolTipText("Open Web Interface in your browser (http://localhost:8080)");
        webBtn.addActionListener(e -> {
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(URI.create("http://localhost:8080"));
                }
            } catch (Exception ignored) {
            }
        });
        controls.add(webBtn);

        header.add(controls, BorderLayout.EAST);
        return header;
    }

    private String getScreenTitle(String key) {
        return switch (key) {
            case "DASHBOARD" -> "Decision Center";
            case "TRANSACTIONS" -> "Cashflow Ledger";
            case "CATEGORIES" -> "Category Control";
            case "BUDGETS" -> "Guardrails & Limits";
            case "REPORTS" -> "Profit & What-If";
            case "SETTINGS" -> "Engine Preferences";
            default -> "SpendWise";
        };
    }

    private class NavButton extends JPanel {
        private final String screenKey;
        private final JLabel label;
        private boolean active = false;
        private boolean hovered = false;

        public NavButton(String screenKey, String text, String tag, Icon icon) {
            this.screenKey = screenKey;
            setLayout(new BorderLayout(10, 0));
            setOpaque(false);
            setMaximumSize(new Dimension(250, 40));
            setPreferredSize(new Dimension(250, 40));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(8, 18, 8, 16));

            label = new JLabel(text.toUpperCase(), icon, JLabel.LEFT);
            label.setIconTextGap(10);
            label.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
            label.setForeground(AppTheme.TEXT_MUTED);

            add(label, BorderLayout.CENTER);

            if (tag != null) {
                JLabel tagLabel = new JLabel(tag);
                tagLabel.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
                tagLabel.setForeground(AppTheme.SECONDARY_ACCENT);
                add(tagLabel, BorderLayout.EAST);
            }

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
            label.setForeground(active ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            if (active) {
                g2.setColor(AppTheme.CARD_RAISE);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.SECONDARY_ACCENT);
                g2.fillRect(0, 0, 3, getHeight());
            } else if (hovered) {
                g2.setColor(new Color(255, 255, 255, 8));
                g2.fillRect(0, 0, getWidth(), getHeight());
            }

            g2.dispose();
            super.paintComponent(g);
        }
    }
}

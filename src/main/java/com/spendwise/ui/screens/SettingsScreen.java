package com.spendwise.ui.screens;

import com.spendwise.database.DatabaseConnection;
import com.spendwise.service.ReportService;
import com.spendwise.service.SpendingAnalysisService;
import com.spendwise.ui.components.AsyncWorker;
import com.spendwise.ui.components.SectionHeader;
import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.components.StyledTextField;
import com.spendwise.ui.components.Toast;
import com.spendwise.ui.theme.AppTheme;
import com.spendwise.util.MoneyUtil;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.math.BigDecimal;

public class SettingsScreen extends JPanel implements BaseScreen {

    private final ReportService reportService;
    private final SpendingAnalysisService analysisService;

    private final JCheckBox reduceAnimationsCheck;
    private final StyledTextField warningThresholdField;
    private final StyledTextField unusualThresholdField;
    private final StyledTextField minIncreaseField;
    private final JLabel dbStatusResultLabel;

    public SettingsScreen(ReportService reportService, SpendingAnalysisService analysisService) {
        this.reportService = reportService;
        this.analysisService = analysisService;

        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(8, 0, 24, 0));

        SectionHeader header = new SectionHeader("Settings & System Diagnostics", "Configure animation preferences, analytical thresholds, and inspect database connectivity");
        content.add(header);

        JPanel prefCard = createSettingsCard("UI & Animation Preferences");
        reduceAnimationsCheck = new JCheckBox("Reduce animations (disables background particles, waves, and glow effects)");
        reduceAnimationsCheck.setSelected(AppTheme.isReduceAnimations());
        reduceAnimationsCheck.setFont(AppTheme.FONT_BODY);
        reduceAnimationsCheck.setForeground(AppTheme.TEXT_PRIMARY);
        reduceAnimationsCheck.setOpaque(false);
        reduceAnimationsCheck.addActionListener(e -> {
            AppTheme.setReduceAnimations(reduceAnimationsCheck.isSelected());
            showToast("Animation settings updated.", Toast.ToastType.SUCCESS);
        });
        prefCard.add(reduceAnimationsCheck);
        content.add(prefCard);

        content.add(Box.createVerticalStrut(18));

        JPanel thresholdCard = createSettingsCard("Analytical Thresholds & Alert Rules");
        JPanel grid = new JPanel(new GridLayout(3, 2, 14, 12));
        grid.setOpaque(false);

        warningThresholdField = new StyledTextField(String.valueOf((int) reportService.getWarningThresholdPercent()), 8);
        unusualThresholdField = new StyledTextField(String.valueOf((int) analysisService.getUnusualSpendingThresholdPercent()), 8);
        minIncreaseField = new StyledTextField(analysisService.getMinAbsoluteIncrease().toPlainString(), 8);

        grid.add(new JLabel("Budget Warning Threshold (%):"));
        grid.add(warningThresholdField);
        grid.add(new JLabel("Unusual Spending Spike Threshold (%):"));
        grid.add(unusualThresholdField);
        grid.add(new JLabel("Minimum Absolute Spike Increase (Rs):"));
        grid.add(minIncreaseField);
        thresholdCard.add(grid);

        thresholdCard.add(Box.createVerticalStrut(14));

        StyledButton saveThresholdsBtn = new StyledButton("Apply Thresholds", StyledButton.ButtonStyle.PRIMARY);
        saveThresholdsBtn.addActionListener(e -> applyThresholds());
        thresholdCard.add(saveThresholdsBtn);

        content.add(thresholdCard);

        content.add(Box.createVerticalStrut(18));

        JPanel dbCard = createSettingsCard("Database Health & Diagnostic Connectivity");
        JPanel dbRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        dbRow.setOpaque(false);

        dbStatusResultLabel = new JLabel("Status: Unknown");
        dbStatusResultLabel.setFont(AppTheme.FONT_BODY_BOLD);
        dbStatusResultLabel.setForeground(AppTheme.TEXT_SECONDARY);

        StyledButton testDbBtn = new StyledButton("Test Connection", StyledButton.ButtonStyle.SECONDARY);
        testDbBtn.addActionListener(e -> testConnection());

        dbRow.add(testDbBtn);
        dbRow.add(dbStatusResultLabel);
        dbCard.add(dbRow);
        content.add(dbCard);

        content.add(Box.createVerticalStrut(18));

        JPanel aboutCard = createSettingsCard("About SpendWise");
        JLabel taglineLbl = new JLabel("SpendWise: Your money. Your clarity.");
        taglineLbl.setFont(AppTheme.FONT_SUBHEADER);
        taglineLbl.setForeground(AppTheme.PRIMARY_ACCENT);

        JLabel aboutText = new JLabel("<html><body style='width: 580px; color: #9CA3AF;'>" +
                "SpendWise is a personal finance management desktop workstation developed for a second-year AIML engineering college project. " +
                "It strictly adheres to pure Java 21, Java Swing, JDBC, MySQL 8, FlatLaf, and JFreeChart without any web or microframework overhead. " +
                "All calculations are rule-based, fully explainable, and persist reliably to local relational storage." +
                "</body></html>");
        aboutText.setFont(AppTheme.FONT_BODY);

        aboutCard.add(taglineLbl);
        aboutCard.add(Box.createVerticalStrut(8));
        aboutCard.add(aboutText);
        content.add(aboutCard);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel createSettingsCard(String title) {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(AppTheme.CARD_BG);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(16, 20, 16, 20)
        ));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_SUBHEADER);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(12));
        return card;
    }

    private void applyThresholds() {
        try {
            double warn = Double.parseDouble(warningThresholdField.getText().trim());
            double spike = Double.parseDouble(unusualThresholdField.getText().trim());
            BigDecimal minInc = MoneyUtil.parse(minIncreaseField.getText().trim());

            reportService.setWarningThresholdPercent(warn);
            analysisService.setUnusualSpendingThresholdPercent(spike);
            analysisService.setMinAbsoluteIncrease(minInc);

            showToast("Threshold settings saved successfully.", Toast.ToastType.SUCCESS);
        } catch (Exception ex) {
            showToast("Error updating thresholds: " + ex.getMessage(), Toast.ToastType.ERROR);
        }
    }

    private void testConnection() {
        dbStatusResultLabel.setText("Pinging database...");
        dbStatusResultLabel.setForeground(AppTheme.TEXT_MUTED);

        AsyncWorker.execute(
                DatabaseConnection::checkConnectivity,
                connected -> {
                    if (connected) {
                        dbStatusResultLabel.setText("Status: Online & Connected (Host: localhost:3306, DB: spendwise)");
                        dbStatusResultLabel.setForeground(AppTheme.SUCCESS);
                    } else {
                        dbStatusResultLabel.setText("Status: Connection Failed (Check MySQL server)");
                        dbStatusResultLabel.setForeground(AppTheme.DANGER);
                    }
                },
                err -> {
                    dbStatusResultLabel.setText("Status: Error (" + err.getMessage() + ")");
                    dbStatusResultLabel.setForeground(AppTheme.DANGER);
                }
        );
    }

    @Override
    public void onScreenSelected() {
        reduceAnimationsCheck.setSelected(AppTheme.isReduceAnimations());
        testConnection();
    }

    @Override
    public void onMonthChanged(int year, int month) {
    }

    private void showToast(String msg, Toast.ToastType type) {
        JFrame topFrame = (JFrame) SwingUtilities.getWindowAncestor(this);
        Toast.show(topFrame, msg, type);
    }
}

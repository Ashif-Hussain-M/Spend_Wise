package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;

public class SummaryCard extends JPanel {

    private final JLabel titleLabel;
    private final JLabel valueLabel;
    private final JLabel subtitleLabel;
    private final JLabel iconLabel;

    public SummaryCard(String title, String initialValue, String subtitle, Icon icon, Color accentColor) {
        setLayout(new BorderLayout(12, 8));
        setBackground(AppTheme.CARD_BG);
        setOpaque(false);
        setBorder(new EmptyBorder(16, 20, 16, 20));
        setPreferredSize(new Dimension(220, 110));

        JPanel textPanel = new JPanel(new GridLayout(3, 1, 0, 4));
        textPanel.setOpaque(false);

        titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_SMALL);
        titleLabel.setForeground(AppTheme.TEXT_SECONDARY);

        valueLabel = new JLabel(initialValue);
        valueLabel.setFont(AppTheme.FONT_KPI);
        valueLabel.setForeground(accentColor != null ? accentColor : AppTheme.TEXT_PRIMARY);

        subtitleLabel = new JLabel(subtitle);
        subtitleLabel.setFont(AppTheme.FONT_SMALL);
        subtitleLabel.setForeground(AppTheme.TEXT_MUTED);

        textPanel.add(titleLabel);
        textPanel.add(valueLabel);
        textPanel.add(subtitleLabel);

        add(textPanel, BorderLayout.CENTER);

        if (icon != null) {
            iconLabel = new JLabel(icon);
            iconLabel.setVerticalAlignment(JLabel.TOP);
            add(iconLabel, BorderLayout.EAST);
        } else {
            iconLabel = null;
        }
    }

    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public String getValue() {
        return valueLabel.getText();
    }

    public void setSubtitle(String subtitle) {
        subtitleLabel.setText(subtitle);
    }

    public void setValueColor(Color color) {
        valueLabel.setForeground(color);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
        g2.setColor(AppTheme.CARD_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
        g2.dispose();
        super.paintComponent(g);
    }
}

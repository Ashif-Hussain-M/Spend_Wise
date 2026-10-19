package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;

public class EmptyStatePanel extends JPanel {

    public EmptyStatePanel(String title, String message, Icon icon, JComponent actionButton) {
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        setBorder(new EmptyBorder(32, 24, 32, 24));

        if (icon != null) {
            JLabel iconLabel = new JLabel(icon);
            iconLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(iconLabel);
            add(Box.createVerticalStrut(12));
        }

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_SUBHEADER);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        add(titleLabel);

        if (message != null && !message.isEmpty()) {
            add(Box.createVerticalStrut(6));
            JLabel msgLabel = new JLabel(message);
            msgLabel.setFont(AppTheme.FONT_BODY);
            msgLabel.setForeground(AppTheme.TEXT_SECONDARY);
            msgLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(msgLabel);
        }

        if (actionButton != null) {
            add(Box.createVerticalStrut(16));
            actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
            add(actionButton);
        }
    }
}

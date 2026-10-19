package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class LoadingPanel extends JPanel {

    public LoadingPanel(String message) {
        setLayout(new BorderLayout());
        setOpaque(false);

        JLabel label = new JLabel(message != null ? message : "Loading data...", JLabel.CENTER);
        label.setFont(AppTheme.FONT_BODY);
        label.setForeground(AppTheme.TEXT_SECONDARY);
        add(label, BorderLayout.CENTER);
    }
}

package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;

public class SectionHeader extends JPanel {

    private final JLabel titleLabel;
    private final JLabel subtitleLabel;
    private final JPanel actionPanel;

    public SectionHeader(String title, String subtitle) {
        setLayout(new BorderLayout());
        setOpaque(false);
        setBorder(new EmptyBorder(8, 0, 16, 0));

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new BoxLayout(textPanel, BoxLayout.Y_AXIS));
        textPanel.setOpaque(false);

        titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_HEADER);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        textPanel.add(titleLabel);

        if (subtitle != null && !subtitle.isEmpty()) {
            textPanel.add(Box.createVerticalStrut(2));
            subtitleLabel = new JLabel(subtitle);
            subtitleLabel.setFont(AppTheme.FONT_SMALL);
            subtitleLabel.setForeground(AppTheme.TEXT_SECONDARY);
            textPanel.add(subtitleLabel);
        } else {
            subtitleLabel = null;
        }

        add(textPanel, BorderLayout.WEST);

        actionPanel = new JPanel();
        actionPanel.setLayout(new BoxLayout(actionPanel, BoxLayout.X_AXIS));
        actionPanel.setOpaque(false);
        add(actionPanel, BorderLayout.EAST);
    }

    public void addAction(JComponent component) {
        if (actionPanel.getComponentCount() > 0) {
            actionPanel.add(Box.createHorizontalStrut(8));
        }
        actionPanel.add(component);
        actionPanel.revalidate();
        actionPanel.repaint();
    }
}

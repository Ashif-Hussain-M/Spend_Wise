package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;

public class ConfirmationDialog extends JDialog {

    private boolean confirmed = false;

    public ConfirmationDialog(JFrame parent, String title, String message, String confirmButtonText, boolean isDanger) {
        super(parent, title, true);
        setLayout(new BorderLayout());
        setResizable(false);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(AppTheme.CARD_BG);
        content.setBorder(new EmptyBorder(20, 24, 20, 24));

        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(AppTheme.FONT_SUBHEADER);
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(titleLabel);

        content.add(Box.createVerticalStrut(10));

        JLabel msgLabel = new JLabel("<html><body style='width: 280px;'>" + message + "</body></html>");
        msgLabel.setFont(AppTheme.FONT_BODY);
        msgLabel.setForeground(AppTheme.TEXT_SECONDARY);
        msgLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(msgLabel);

        content.add(Box.createVerticalStrut(20));

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttonPanel.setOpaque(false);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);

        StyledButton cancelButton = new StyledButton("Cancel", StyledButton.ButtonStyle.SECONDARY);
        cancelButton.addActionListener(e -> {
            confirmed = false;
            dispose();
        });

        StyledButton confirmButton = new StyledButton(
                confirmButtonText != null ? confirmButtonText : "Confirm",
                isDanger ? StyledButton.ButtonStyle.DANGER : StyledButton.ButtonStyle.PRIMARY
        );
        confirmButton.addActionListener(e -> {
            confirmed = true;
            dispose();
        });

        buttonPanel.add(cancelButton);
        buttonPanel.add(confirmButton);
        content.add(buttonPanel);

        add(content, BorderLayout.CENTER);
        getRootPane().setBorder(BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1));
        pack();
        setLocationRelativeTo(parent);
    }

    public static boolean show(JFrame parent, String title, String message, String confirmButtonText, boolean isDanger) {
        ConfirmationDialog dlg = new ConfirmationDialog(parent, title, message, confirmButtonText, isDanger);
        dlg.setVisible(true);
        return dlg.confirmed;
    }
}
